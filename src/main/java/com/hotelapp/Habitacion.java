package com.hotelapp;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Modelo de una habitación del hotel.
 *
 * <p>Además de los datos físicos de la habitación (tipo, capacidad, planta y
 * precio), el modelo incorpora el estado operativo y la información de la
 * reserva vigente. El estado se normaliza al crear el objeto para que la
 * aplicación trabaje siempre con valores internos homogéneos:</p>
 *
 * <ul>
 *   <li>{@code disponible}: la habitación puede reservarse.</li>
 *   <li>{@code reservada}: tiene una reserva confirmada pendiente de entrada.</li>
 *   <li>{@code ocupada}: el huésped ya ha realizado el check-in.</li>
 *   <li>{@code mantenimiento}: fuera de servicio temporalmente.</li>
 * </ul>
 */
public final class Habitacion {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int numero;
    private final String estado;
    private final String tipo;
    private final int capacidad;
    private final int planta;
    private final double precio;
    private final String cliente;
    private final String documentoContacto;
    private final LocalDate fechaCheckIn;
    private final LocalDate fechaCheckOut;
    private final String estadoReserva;
    private final String reservaId;
    private final String nota;

    /**
     * Constructor completo utilizado por la capa de persistencia.
     */
    public Habitacion(int numero, String estado, String tipo, int capacidad,
                      int planta, double precio, String cliente, String documentoContacto,
                      LocalDate fechaCheckIn, LocalDate fechaCheckOut, String estadoReserva,
                      String reservaId, String nota) {
        this.numero = numero;
        this.estado = normalizarEstado(estado);
        this.tipo = tipo == null || tipo.isBlank() ? "Sin clasificar" : tipo.trim();
        this.capacidad = Math.max(capacidad, 0);
        this.planta = planta;
        this.precio = Math.max(precio, 0);
        this.cliente = cliente == null ? "" : cliente.trim();
        this.documentoContacto = documentoContacto == null ? "" : documentoContacto.trim();
        this.fechaCheckIn = fechaCheckIn;
        this.fechaCheckOut = fechaCheckOut;
        this.estadoReserva = Reserva.normalizarEstado(estadoReserva);
        this.reservaId = reservaId == null ? "" : reservaId.trim();
        this.nota = nota == null ? "" : nota.trim();
    }

    /**
     * Constructor de compatibilidad con los datos de la Parte 1.
     */
    public Habitacion(int numero, String estado, String tipo, int capacidad,
                      int planta, double precio, String cliente, String nota) {
        this(numero, estado, tipo, capacidad, planta, precio, cliente, "", null,
                null, "", "", nota);
    }

    /**
     * Convierte los valores del XML a un estado interno homogéneo.
     * Se admite "libre" como sinónimo de "disponible" y se ignoran tildes
     * y mayúsculas para que el parser sea tolerante.
     */
    public static String normalizarEstado(String valor) {
        if (valor == null) {
            return "desconocido";
        }

        String limpio = Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return switch (limpio) {
            case "disponible", "libre", "libres" -> "disponible";
            case "reservada", "reservado", "reservadas", "reservados" -> "reservada";
            case "ocupada", "ocupado", "ocupadas", "ocupados" -> "ocupada";
            case "mantenimiento", "en mantenimiento", "fuera de servicio" -> "mantenimiento";
            default -> "desconocido";
        };
    }

    public int getNumero() {
        return numero;
    }

    public String getNumeroTexto() {
        return String.valueOf(numero);
    }

    public String getEstado() {
        return estado;
    }

    public String getEstadoEtiqueta() {
        return switch (estado) {
            case "disponible" -> "Disponible";
            case "reservada" -> "Reservada";
            case "ocupada" -> "Ocupada";
            case "mantenimiento" -> "En mantenimiento";
            default -> "Desconocido";
        };
    }

    public boolean estaDisponible() {
        return "disponible".equals(estado);
    }

    public boolean estaReservada() {
        return "reservada".equals(estado);
    }

    public boolean estaOcupada() {
        return "ocupada".equals(estado);
    }

    public boolean estaEnMantenimiento() {
        return "mantenimiento".equals(estado);
    }

    public boolean tieneReservaActiva() {
        return estaReservada() || estaOcupada();
    }

    public String getTipo() {
        return tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public String getCapacidadTexto() {
        if (capacidad <= 0) {
            return "Sin datos";
        }
        return capacidad == 1 ? "1 persona" : capacidad + " personas";
    }

    public int getPlanta() {
        return planta;
    }

    public String getPlantaTexto() {
        return "Planta " + planta;
    }

    public double getPrecio() {
        return precio;
    }

    public String getPrecioTexto() {
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f €", precio);
    }

    public String getCliente() {
        return cliente;
    }

    public String getClienteTexto() {
        return cliente == null || cliente.isBlank() ? "Sin asignar" : cliente;
    }

    public String getDocumentoContacto() {
        return documentoContacto;
    }

    public String getDocumentoContactoTexto() {
        return documentoContacto == null || documentoContacto.isBlank()
                ? "Sin datos de contacto"
                : documentoContacto;
    }

    public LocalDate getFechaCheckIn() {
        return fechaCheckIn;
    }

    public LocalDate getFechaCheckOut() {
        return fechaCheckOut;
    }

    public String getPeriodoTexto() {
        if (fechaCheckIn == null && fechaCheckOut == null) {
            return "Sin fechas de estancia";
        }
        String entrada = fechaCheckIn == null ? "—" : fechaCheckIn.format(FORMATO_FECHA);
        String salida = fechaCheckOut == null ? "—" : fechaCheckOut.format(FORMATO_FECHA);
        return entrada + "  →  " + salida;
    }

    public String getEstadoReserva() {
        return estadoReserva;
    }

    public String getEstadoReservaTexto() {
        return Reserva.etiquetaEstado(estadoReserva);
    }

    public String getReservaId() {
        return reservaId;
    }

    public String getNota() {
        return nota;
    }

    public String getNotaTexto() {
        return nota == null || nota.isBlank() ? "Sin observaciones" : nota;
    }

    @Override
    public String toString() {
        return "Habitación " + numero + " (" + getEstadoEtiqueta() + ")";
    }
}
