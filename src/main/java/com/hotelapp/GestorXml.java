package com.hotelapp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de persistencia XML basado en W3C DOM.
 *
 * <p>La clase centraliza toda la lectura y escritura de {@code reservas.xml}.
 * La lectura se realiza con {@link DocumentBuilderFactory} y la escritura con
 * {@link TransformerFactory} + {@link Transformer}, serializando un
 * {@link DOMSource} sobre un {@link StreamResult}. De este modo, cada alta de
 * reserva, check-in, check-out o cancelación queda persistida en disco de forma
 * atómica, sin depender de bases de datos externas.</p>
 */
public final class GestorXml {

    private static final String NOMBRE_ARCHIVO = "reservas.xml";
    private static final String TAG_RESERVAS = "reservas";
    private static final Pattern PATRON_ID = Pattern.compile("RES-(\\d+)");

    private final Path rutaXml;
    private final Path rutaEspejo;

    public GestorXml() {
        this.rutaXml = resolverRutaXml();
        this.rutaEspejo = resolverRutaEspejo(this.rutaXml);
    }

    public Path getRutaXml() {
        return rutaXml;
    }

    // =========================================================================
    // LECTURA
    // =========================================================================

    /**
     * Carga el hotel completo: metadatos, habitaciones y reservas.
     */
    public synchronized DatosHotel cargar() throws GestorXmlException {
        Document documento = leerDocumento();
        Element raiz = documento.getDocumentElement();

        String nombreHotel = valorAtributo(raiz, "nombre", "Hotel Sol");
        String ciudad = valorAtributo(raiz, "ciudad", "Madrid");
        int estrellas = enteroSeguro(valorAtributo(raiz, "estrellas", "4"), 4);

        List<Habitacion> habitaciones = leerHabitaciones(documento);
        List<Reserva> reservas = leerReservas(documento);

        if (reservas.isEmpty()) {
            reservas = reservasDesdeHabitacionesOcupadas(habitaciones);
        }

        return new DatosHotel(nombreHotel, ciudad, estrellas, habitaciones, reservas);
    }

    private List<Habitacion> leerHabitaciones(Document documento) {
        NodeList nodos = documento.getElementsByTagName("habitacion");
        List<Habitacion> resultado = new ArrayList<>();

        for (int i = 0; i < nodos.getLength(); i++) {
            if (!(nodos.item(i) instanceof Element elemento)) {
                continue;
            }

            try {
                String numeroTexto = valorAtributo(elemento, "numero", "");
                if (numeroTexto.isBlank()) {
                    numeroTexto = textoHijo(elemento, "numero");
                }
                int numero = Integer.parseInt(numeroTexto.trim());
                if (numero <= 0) {
                    continue;
                }

                String estado = valorAtributo(elemento, "estado", "");
                if (estado.isBlank()) {
                    estado = textoHijo(elemento, "estado");
                }

                String tipo = valorHijo(elemento, "tipo", "Sin clasificar");
                int capacidad = enteroSeguro(valorHijo(elemento, "capacidad", "0"), 0);
                int planta = enteroSeguro(valorHijo(elemento, "planta", "1"), 1);
                double precio = decimalSeguro(valorHijo(elemento, "precio", "0"), 0);
                String cliente = valorHijo(elemento, "cliente", "");
                String documentoContacto = valorHijo(elemento, "documentoContacto", "");
                LocalDate checkIn = fechaSegura(valorHijo(elemento, "fechaCheckIn", ""));
                LocalDate checkOut = fechaSegura(valorHijo(elemento, "fechaCheckOut", ""));
                String estadoReserva = valorHijo(elemento, "estadoReserva", "");
                String reservaId = valorHijo(elemento, "reservaId", "");
                String nota = valorHijo(elemento, "nota", "");

                String estadoNormalizado = Habitacion.normalizarEstado(estado);
                if ("desconocido".equals(estadoNormalizado) && !cliente.isBlank()) {
                    estadoNormalizado = "ocupada";
                }
                if (estadoReserva.isBlank() && !reservaId.isBlank()) {
                    estadoReserva = "ocupada".equals(estadoNormalizado) ? "alojada" : "confirmada";
                }

                resultado.add(new Habitacion(numero, estadoNormalizado, tipo, capacidad, planta,
                        precio, cliente, documentoContacto, checkIn, checkOut, estadoReserva,
                        reservaId, nota));
            } catch (NumberFormatException ex) {
                System.err.println("Habitación ignorada por número no válido: " + ex.getMessage());
            }
        }

        resultado.sort((a, b) -> Integer.compare(a.getNumero(), b.getNumero()));
        return resultado;
    }

    private List<Reserva> leerReservas(Document documento) {
        NodeList nodos = documento.getElementsByTagName("reserva");
        List<Reserva> resultado = new ArrayList<>();

        for (int i = 0; i < nodos.getLength(); i++) {
            if (!(nodos.item(i) instanceof Element elemento)) {
                continue;
            }
            Reserva reserva = leerReserva(elemento);
            if (reserva != null) {
                resultado.add(reserva);
            }
        }

        resultado.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return resultado;
    }

    private Reserva leerReserva(Element elemento) {
        try {
            String id = valorAtributo(elemento, "id", valorHijo(elemento, "id", ""));
            String habitacionTexto = valorAtributo(elemento, "habitacion", valorHijo(elemento, "habitacion", ""));
            int numeroHabitacion = Integer.parseInt(habitacionTexto.trim());
            if (id.isBlank() || numeroHabitacion <= 0) {
                return null;
            }

            String cliente = valorHijo(elemento, "cliente", "");
            String documentoContacto = valorHijo(elemento, "documentoContacto", "");
            LocalDate checkIn = fechaSegura(valorHijo(elemento, "fechaCheckIn", ""));
            LocalDate checkOut = fechaSegura(valorHijo(elemento, "fechaCheckOut", ""));
            String estado = valorHijo(elemento, "estado", valorAtributo(elemento, "estado", ""));
            double precioNoche = decimalSeguro(
                    valorAtributo(elemento, "precioNoche", valorHijo(elemento, "precioNoche", "0")), 0);
            LocalDate creadaEn = fechaSegura(valorAtributo(elemento, "creadaEn",
                    valorHijo(elemento, "creadaEn", "")));
            String nota = valorHijo(elemento, "nota", "");

            return new Reserva(id, numeroHabitacion, cliente, documentoContacto, checkIn, checkOut,
                    estado, precioNoche, creadaEn, nota);
        } catch (NumberFormatException ex) {
            System.err.println("Reserva ignorada por datos no válidos: " + ex.getMessage());
            return null;
        }
    }

    private List<Reserva> reservasDesdeHabitacionesOcupadas(List<Habitacion> habitaciones) {
        List<Reserva> resultado = new ArrayList<>();
        int secuencia = 1;

        for (Habitacion habitacion : habitaciones) {
            if (!habitacion.estaOcupada() || habitacion.getCliente().isBlank()) {
                continue;
            }

            LocalDate entrada = habitacion.getFechaCheckIn() != null
                    ? habitacion.getFechaCheckIn()
                    : LocalDate.now().minusDays(1);
            LocalDate salida = habitacion.getFechaCheckOut() != null
                    ? habitacion.getFechaCheckOut()
                    : LocalDate.now().plusDays(2);
            String id = habitacion.getReservaId().isBlank()
                    ? String.format(Locale.ROOT, "RES-%04d", secuencia++)
                    : habitacion.getReservaId();

            resultado.add(new Reserva(id, habitacion.getNumero(), habitacion.getCliente(),
                    habitacion.getDocumentoContacto(), entrada, salida, "alojada",
                    habitacion.getPrecio(), entrada, habitacion.getNota()));
        }

        return resultado;
    }

    // =========================================================================
    // ESCRITURA / OPERACIONES DE NEGOCIO
    // =========================================================================

    /**
     * Crea una reserva confirmada, actualiza la habitación y persiste el XML.
     */
    public synchronized Reserva crearReserva(int numeroHabitacion, String cliente,
                                             String documentoContacto, LocalDate checkIn,
                                             LocalDate checkOut) throws GestorXmlException {
        validarDatosReserva(numeroHabitacion, cliente, documentoContacto, checkIn, checkOut);

        Document documento = leerDocumento();
        Element habitacion = buscarHabitacion(documento, numeroHabitacion);
        if (habitacion == null) {
            throw new GestorXmlException("La habitación " + numeroHabitacion + " no existe.");
        }

        String estadoHabitacion = Habitacion.normalizarEstado(leerEstadoHabitacion(habitacion));
        if ("mantenimiento".equals(estadoHabitacion)) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " está en mantenimiento y no puede reservarse.");
        }
        if ("ocupada".equals(estadoHabitacion)) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " está ocupada y no puede reservarse.");
        }

        if (reservaActivaDeHabitacion(documento, numeroHabitacion) != null) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " ya tiene una reserva activa.");
        }

        String id = siguienteId(documento);
        double precioNoche = decimalSeguro(valorHijo(habitacion, "precio", "0"), 0);
        Element reserva = documento.createElement("reserva");
        reserva.setAttribute("id", id);
        reserva.setAttribute("habitacion", String.valueOf(numeroHabitacion));
        reserva.setAttribute("precioNoche", String.format(Locale.ROOT, "%.2f", precioNoche));
        reserva.setAttribute("creadaEn", LocalDate.now().toString());

        agregarHijo(documento, reserva, "cliente", cliente.trim());
        agregarHijo(documento, reserva, "documentoContacto", documentoContacto.trim());
        agregarHijo(documento, reserva, "fechaCheckIn", checkIn.toString());
        agregarHijo(documento, reserva, "fechaCheckOut", checkOut.toString());
        agregarHijo(documento, reserva, "estado", "confirmada");
        agregarHijo(documento, reserva, "nota", "Reserva confirmada. Pendiente de check-in.");

        asegurarSeccionReservas(documento).appendChild(reserva);

        actualizarHabitacionConReserva(documento, habitacion, "reservada",
                cliente, documentoContacto, checkIn, checkOut, "confirmada", id);

        guardar(documento);

        return new Reserva(id, numeroHabitacion, cliente, documentoContacto, checkIn, checkOut,
                "confirmada", precioNoche, LocalDate.now(),
                "Reserva confirmada. Pendiente de check-in.");
    }

    /**
     * Realiza el check-in de una reserva confirmada y ocupa la habitación.
     */
    public synchronized Reserva realizarCheckIn(String idReserva) throws GestorXmlException {
        Document documento = leerDocumento();
        Element reserva = buscarReserva(documento, idReserva);
        if (reserva == null) {
            throw new GestorXmlException("No se encontró la reserva " + idReserva + ".");
        }

        String estadoReserva = Reserva.normalizarEstado(leerEstadoReserva(reserva));
        if (!"confirmada".equals(estadoReserva)) {
            throw new GestorXmlException("La reserva " + idReserva
                    + " no está confirmada. Estado actual: "
                    + Reserva.etiquetaEstado(estadoReserva) + ".");
        }

        int numeroHabitacion = leerNumeroHabitacionReserva(reserva);
        Element habitacion = buscarHabitacion(documento, numeroHabitacion);
        if (habitacion == null) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " asociada a la reserva no existe.");
        }

        String estadoHabitacion = Habitacion.normalizarEstado(leerEstadoHabitacion(habitacion));
        if ("mantenimiento".equals(estadoHabitacion)) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " está en mantenimiento. No se puede hacer check-in.");
        }
        if ("ocupada".equals(estadoHabitacion)) {
            throw new GestorXmlException("La habitación " + numeroHabitacion + " ya está ocupada.");
        }

        LocalDate entrada = fechaSegura(valorHijo(reserva, "fechaCheckIn", ""));
        if (entrada != null && LocalDate.now().isBefore(entrada)) {
            throw new GestorXmlException("No se puede hacer check-in antes del "
                    + entrada.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        }

        String cliente = valorHijo(reserva, "cliente", "");
        String documentoContacto = valorHijo(reserva, "documentoContacto", "");
        LocalDate salida = fechaSegura(valorHijo(reserva, "fechaCheckOut", ""));

        actualizarHabitacionConReserva(documento, habitacion, "ocupada",
                cliente, documentoContacto, entrada, salida, "alojada", idReserva);
        setHijoTexto(documento, reserva, "estado", "alojada");
        setHijoTexto(documento, reserva, "nota", "Check-in realizado. Huésped alojado.");

        guardar(documento);
        return leerReserva(reserva);
    }

    /**
     * Realiza el check-out, libera la habitación y finaliza la reserva.
     */
    public synchronized Reserva realizarCheckOut(String idReserva) throws GestorXmlException {
        Document documento = leerDocumento();
        Element reserva = buscarReserva(documento, idReserva);
        if (reserva == null) {
            throw new GestorXmlException("No se encontró la reserva " + idReserva + ".");
        }

        String estadoReserva = Reserva.normalizarEstado(leerEstadoReserva(reserva));
        if (!"alojada".equals(estadoReserva)) {
            throw new GestorXmlException("La reserva " + idReserva
                    + " no está en curso. Estado actual: "
                    + Reserva.etiquetaEstado(estadoReserva) + ".");
        }

        int numeroHabitacion = leerNumeroHabitacionReserva(reserva);
        Element habitacion = buscarHabitacion(documento, numeroHabitacion);
        if (habitacion == null) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " asociada a la reserva no existe.");
        }

        String estadoHabitacion = Habitacion.normalizarEstado(leerEstadoHabitacion(habitacion));
        if (!"ocupada".equals(estadoHabitacion)) {
            throw new GestorXmlException("La habitación " + numeroHabitacion
                    + " no está ocupada. No se puede hacer check-out.");
        }

        habitacion.setAttribute("estado", "disponible");
        limpiarDatosReservaHabitacion(habitacion);
        setHijoTexto(documento, reserva, "estado", "finalizada");
        setHijoTexto(documento, reserva, "nota", "Check-out realizado. Habitación liberada.");

        guardar(documento);
        return leerReserva(reserva);
    }

    /**
     * Cancela una reserva confirmada o pendiente y libera la habitación.
     */
    public synchronized Reserva cancelarReserva(String idReserva) throws GestorXmlException {
        Document documento = leerDocumento();
        Element reserva = buscarReserva(documento, idReserva);
        if (reserva == null) {
            throw new GestorXmlException("No se encontró la reserva " + idReserva + ".");
        }

        String estadoReserva = Reserva.normalizarEstado(leerEstadoReserva(reserva));
        if ("alojada".equals(estadoReserva)) {
            throw new GestorXmlException("La reserva está en curso. Realiza primero el check-out.");
        }
        if (!"confirmada".equals(estadoReserva) && !"pendiente".equals(estadoReserva)) {
            throw new GestorXmlException("La reserva " + idReserva
                    + " ya está " + Reserva.etiquetaEstado(estadoReserva).toLowerCase(Locale.ROOT) + ".");
        }

        int numeroHabitacion = leerNumeroHabitacionReserva(reserva);
        Element habitacion = buscarHabitacion(documento, numeroHabitacion);

        setHijoTexto(documento, reserva, "estado", "cancelada");
        setHijoTexto(documento, reserva, "nota", "Reserva cancelada por recepción.");

        if (habitacion != null) {
            String reservaIdHabitacion = valorHijo(habitacion, "reservaId", "");
            if (idReserva.equalsIgnoreCase(reservaIdHabitacion)) {
                habitacion.setAttribute("estado", "disponible");
                limpiarDatosReservaHabitacion(habitacion);
            }
        }

        guardar(documento);
        return leerReserva(reserva);
    }

    // =========================================================================
    // ESCRITURA DOM -> DISCO
    // =========================================================================

    private Document leerDocumento() throws GestorXmlException {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(false);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            fabrica.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            try {
                fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (IllegalArgumentException ignored) {
                // Algunas implementaciones no exponen estos atributos.
            }

            try (InputStream entrada = Files.newInputStream(rutaXml)) {
                Document documento = fabrica.newDocumentBuilder().parse(entrada);
                documento.getDocumentElement().normalize();
                return documento;
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new GestorXmlException("No se pudo leer " + NOMBRE_ARCHIVO
                    + " (" + rutaXml + "): " + ex.getMessage(), ex);
        }
    }

    private void guardar(Document documento) throws GestorXmlException {
        Element raiz = documento.getDocumentElement();
        raiz.setAttribute("actualizado", LocalDate.now().toString());
        limpiarEspaciosEnBlanco(raiz);

        Path temporal = null;
        try {
            TransformerFactory fabrica = TransformerFactory.newInstance();
            try {
                fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (IllegalArgumentException | TransformerException ignored) {
                // Configuración opcional según la implementación del Transformer.
            }

            Transformer transformer = fabrica.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            Path directorio = rutaXml.toAbsolutePath().getParent();
            temporal = Files.createTempFile(directorio, "reservas-", ".xml");
            transformer.transform(new DOMSource(documento), new StreamResult(temporal.toFile()));
            normalizarSeparadoresDocumento(temporal);

            try {
                Files.move(temporal, rutaXml, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, rutaXml, StandardCopyOption.REPLACE_EXISTING);
            }

            sincronizarEspejo();
        } catch (IOException | TransformerException ex) {
            throw new GestorXmlException("No se pudo guardar " + NOMBRE_ARCHIVO
                    + " (" + rutaXml + "): " + ex.getMessage(), ex);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignored) {
                    // El archivo temporal se eliminará en la próxima limpieza del sistema.
                }
            }
        }
    }

    private void sincronizarEspejo() throws IOException {
        if (rutaEspejo == null || rutaEspejo.equals(rutaXml)) {
            return;
        }
        Files.createDirectories(rutaEspejo.toAbsolutePath().getParent());
        Files.copy(rutaXml, rutaEspejo, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * El serializador deja el primer comentario pegado a la raíz. Se separa
     * únicamente ese límite para conservar una salida legible sin alterar el
     * contenido ni los comentarios internos del documento.
     */
    private static void normalizarSeparadoresDocumento(Path archivo) throws IOException {
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        if (contenido.contains("--><")) {
            contenido = contenido.replaceFirst("--><", "-->\n<");
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        }
    }

    // =========================================================================
    // ACTUALIZACIÓN DE NODOS
    // =========================================================================

    private void actualizarHabitacionConReserva(Document documento, Element habitacion,
                                                String nuevoEstado,
                                                String cliente, String documentoContacto,
                                                LocalDate checkIn, LocalDate checkOut,
                                                String estadoReserva, String reservaId) {
        habitacion.setAttribute("estado", nuevoEstado);
        setHijoTexto(documento, habitacion, "cliente", cliente == null ? "" : cliente.trim());
        setHijoTexto(documento, habitacion, "documentoContacto",
                documentoContacto == null ? "" : documentoContacto.trim());
        setHijoTexto(documento, habitacion, "fechaCheckIn", checkIn == null ? "" : checkIn.toString());
        setHijoTexto(documento, habitacion, "fechaCheckOut", checkOut == null ? "" : checkOut.toString());
        setHijoTexto(documento, habitacion, "estadoReserva", estadoReserva);
        setHijoTexto(documento, habitacion, "reservaId", reservaId);
        setHijoTexto(documento, habitacion, "nota",
                "ocupada".equals(nuevoEstado) ? "Check-in realizado." : "Reserva confirmada.");
    }

    private void limpiarDatosReservaHabitacion(Element habitacion) {
        eliminarHijo(habitacion, "cliente");
        eliminarHijo(habitacion, "documentoContacto");
        eliminarHijo(habitacion, "fechaCheckIn");
        eliminarHijo(habitacion, "fechaCheckOut");
        eliminarHijo(habitacion, "estadoReserva");
        eliminarHijo(habitacion, "reservaId");
        eliminarHijo(habitacion, "nota");
    }

    private Element asegurarSeccionReservas(Document documento) {
        Element raiz = documento.getDocumentElement();
        Element seccion = primerHijo(raiz, TAG_RESERVAS);
        if (seccion == null) {
            seccion = documento.createElement(TAG_RESERVAS);
            raiz.appendChild(seccion);
        }
        return seccion;
    }

    private String siguienteId(Document documento) {
        Set<Integer> usados = new TreeSet<>();
        NodeList nodos = documento.getElementsByTagName("reserva");

        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i) instanceof Element elemento) {
                Matcher matcher = PATRON_ID.matcher(valorAtributo(elemento, "id",
                        valorHijo(elemento, "id", "")));
                if (matcher.matches()) {
                    usados.add(Integer.parseInt(matcher.group(1)));
                }
            }
        }

        int siguiente = usados.isEmpty() ? 1 : usados.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        return String.format(Locale.ROOT, "RES-%04d", siguiente);
    }

    // =========================================================================
    // BÚSQUEDA DE NODOS
    // =========================================================================

    private Element buscarHabitacion(Document documento, int numero) {
        NodeList nodos = documento.getElementsByTagName("habitacion");
        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i) instanceof Element elemento
                    && String.valueOf(numero).equals(valorAtributo(elemento, "numero",
                    textoHijo(elemento, "numero")))) {
                return elemento;
            }
        }
        return null;
    }

    private Element buscarReserva(Document documento, String id) {
        NodeList nodos = documento.getElementsByTagName("reserva");
        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i) instanceof Element elemento
                    && id.equalsIgnoreCase(valorAtributo(elemento, "id", valorHijo(elemento, "id", "")))) {
                return elemento;
            }
        }
        return null;
    }

    private Element reservaActivaDeHabitacion(Document documento, int numeroHabitacion) {
        NodeList nodos = documento.getElementsByTagName("reserva");
        for (int i = 0; i < nodos.getLength(); i++) {
            if (!(nodos.item(i) instanceof Element elemento)) {
                continue;
            }
            int numero = enteroSeguro(valorAtributo(elemento, "habitacion",
                    valorHijo(elemento, "habitacion", "0")), 0);
            if (numero == numeroHabitacion && reservaActiva(elemento)) {
                return elemento;
            }
        }
        return null;
    }

    private boolean reservaActiva(Element reserva) {
        String estado = Reserva.normalizarEstado(leerEstadoReserva(reserva));
        return "pendiente".equals(estado) || "confirmada".equals(estado) || "alojada".equals(estado);
    }

    private String leerEstadoHabitacion(Element habitacion) {
        String estado = valorAtributo(habitacion, "estado", "");
        return estado.isBlank() ? textoHijo(habitacion, "estado") : estado;
    }

    private String leerEstadoReserva(Element reserva) {
        String estado = valorHijo(reserva, "estado", "");
        return estado.isBlank() ? valorAtributo(reserva, "estado", "") : estado;
    }

    private int leerNumeroHabitacionReserva(Element reserva) {
        return enteroSeguro(valorAtributo(reserva, "habitacion",
                valorHijo(reserva, "habitacion", "0")), 0);
    }

    // =========================================================================
    // VALIDACIONES
    // =========================================================================

    private void validarDatosReserva(int numeroHabitacion, String cliente,
                                     String documentoContacto, LocalDate checkIn,
                                     LocalDate checkOut) throws GestorXmlException {
        if (numeroHabitacion <= 0) {
            throw new GestorXmlException("El número de habitación debe ser positivo.");
        }
        if (cliente == null || cliente.trim().length() < 3) {
            throw new GestorXmlException("El nombre del cliente debe tener al menos 3 caracteres.");
        }
        if (documentoContacto == null || documentoContacto.trim().length() < 5) {
            throw new GestorXmlException("Indica un DNI o teléfono de contacto válido.");
        }
        if (checkIn == null || checkOut == null) {
            throw new GestorXmlException("Debes indicar las fechas de entrada y salida.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new GestorXmlException("La fecha de salida debe ser posterior a la de entrada.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new GestorXmlException("La fecha de entrada no puede ser anterior a hoy.");
        }
    }

    // =========================================================================
    // UTILIDADES DOM
    // =========================================================================

    private static Element primerHijo(Element padre, String etiqueta) {
        for (Node nodo = padre.getFirstChild(); nodo != null; nodo = nodo.getNextSibling()) {
            if (nodo instanceof Element elemento && etiqueta.equals(elemento.getTagName())) {
                return elemento;
            }
        }
        return null;
    }

    private static void agregarHijo(Document documento, Element padre, String etiqueta, String valor) {
        Element hijo = documento.createElement(etiqueta);
        hijo.setTextContent(valor == null ? "" : valor);
        padre.appendChild(hijo);
    }

    private static void setHijoTexto(Document documento, Element padre, String etiqueta, String valor) {
        Element hijo = primerHijo(padre, etiqueta);
        if (hijo == null) {
            agregarHijo(documento, padre, etiqueta, valor);
        } else {
            hijo.setTextContent(valor == null ? "" : valor);
        }
    }

    private static void eliminarHijo(Element padre, String etiqueta) {
        Element hijo = primerHijo(padre, etiqueta);
        if (hijo != null) {
            padre.removeChild(hijo);
        }
    }

    private static String textoHijo(Element padre, String etiqueta) {
        Element hijo = primerHijo(padre, etiqueta);
        return hijo == null ? "" : hijo.getTextContent().trim();
    }

    private static String valorHijo(Element padre, String etiqueta, String porDefecto) {
        String valor = textoHijo(padre, etiqueta);
        return valor.isBlank() ? porDefecto : valor;
    }

    private static String valorAtributo(Element elemento, String nombre, String porDefecto) {
        String valor = elemento.getAttribute(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    private static int enteroSeguro(String valor, int porDefecto) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    private static double decimalSeguro(String valor, double porDefecto) {
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    private static LocalDate fechaSegura(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static void limpiarEspaciosEnBlanco(Node nodo) {
        if (nodo.getNodeType() == Node.ELEMENT_NODE) {
            Node hijo = nodo.getFirstChild();
            while (hijo != null) {
                Node siguiente = hijo.getNextSibling();
                if (hijo.getNodeType() == Node.TEXT_NODE && hijo.getTextContent().isBlank()) {
                    nodo.removeChild(hijo);
                } else {
                    limpiarEspaciosEnBlanco(hijo);
                }
                hijo = siguiente;
            }
        }
    }

    // =========================================================================
    // RESOLUCIÓN DE RUTAS
    // =========================================================================

    private static Path resolverRutaXml() {
        String rutaConfigurada = System.getProperty("hotelapp.xml");
        if (rutaConfigurada != null && !rutaConfigurada.isBlank()) {
            return Path.of(rutaConfigurada).toAbsolutePath().normalize();
        }

        Path fuente = Path.of("src", "main", "resources", NOMBRE_ARCHIVO).toAbsolutePath().normalize();
        if (Files.isRegularFile(fuente)) {
            return fuente;
        }

        Path clases = Path.of("target", "classes", NOMBRE_ARCHIVO).toAbsolutePath().normalize();
        if (Files.isRegularFile(clases)) {
            return clases;
        }

        Path local = Path.of(NOMBRE_ARCHIVO).toAbsolutePath().normalize();
        if (Files.isRegularFile(local)) {
            return local;
        }

        Path usuario = Path.of(System.getProperty("user.home"), ".hotelapp", NOMBRE_ARCHIVO)
                .toAbsolutePath().normalize();
        if (Files.isRegularFile(usuario)) {
            return usuario;
        }

        try (InputStream entrada = GestorXml.class.getResourceAsStream("/" + NOMBRE_ARCHIVO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró " + NOMBRE_ARCHIVO
                        + " en resources ni en el sistema de archivos.");
            }
            Files.createDirectories(usuario.getParent());
            Files.copy(entrada, usuario, StandardCopyOption.REPLACE_EXISTING);
            return usuario;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo preparar " + NOMBRE_ARCHIVO
                    + " en " + usuario + ": " + ex.getMessage(), ex);
        }
    }

    private static Path resolverRutaEspejo(Path rutaPrincipal) {
        Path fuente = Path.of("src", "main", "resources", NOMBRE_ARCHIVO).toAbsolutePath().normalize();
        Path clases = Path.of("target", "classes", NOMBRE_ARCHIVO).toAbsolutePath().normalize();

        if (rutaPrincipal.equals(clases) && Files.isRegularFile(fuente)) {
            return fuente;
        }
        if (rutaPrincipal.equals(fuente) && Files.isDirectory(clases.getParent())) {
            return clases;
        }
        return null;
    }

    // =========================================================================
    // TIPOS DE APOYO
    // =========================================================================

    /**
     * Instantánea completa del contenido del hotel.
     */
    public record DatosHotel(String nombreHotel, String ciudad, int estrellas,
                             List<Habitacion> habitaciones, List<Reserva> reservas) {
        public DatosHotel {
            habitaciones = List.copyOf(habitaciones);
            reservas = List.copyOf(reservas);
        }
    }

    /**
     * Error controlado de lectura, validación o escritura del XML.
     */
    public static class GestorXmlException extends Exception {
        public GestorXmlException(String mensaje) {
            super(mensaje);
        }

        public GestorXmlException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
