package com.hotelapp;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador principal de HotelApp v2.
 *
 * <p>Coordina la navegación por pestañas, la sincronización reactiva entre las
 * tablas de JavaFX y el fichero {@code reservas.xml}, y todas las operaciones
 * de recepción: alta de reserva, check-in, check-out y cancelación.</p>
 *
 * <p>La vista no contiene lógica de negocio ni CSS externo: los estilos
 * puntuales viven en el FXML sobre el tema Modena, y toda la persistencia se
 * delega en {@link GestorXml}.</p>
 */
public class Controlador implements Initializable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");

    private static final String BASE_MENSAJE =
            "-fx-padding: 10 14 10 14; -fx-background-radius: 6; -fx-border-radius: 6; "
                    + "-fx-border-width: 1; -fx-font-size: 12px; -fx-font-weight: bold;";
    private static final String ESTILO_EXITO = BASE_MENSAJE
            + "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-border-color: #86EFAC;";
    private static final String ESTILO_ERROR = BASE_MENSAJE
            + "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-border-color: #FCA5A5;";
    private static final String ESTILO_AVISO = BASE_MENSAJE
            + "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-border-color: #FCD34D;";
    private static final String ESTILO_INFO = BASE_MENSAJE
            + "-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-border-color: #BFDBFE;";
    private static final String ESTILO_NEUTRO = BASE_MENSAJE
            + "-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-border-color: #CBD5E1;";

    private static final String ESTILO_PANEL_BASE =
            "-fx-padding: 12 16 12 16; -fx-background-radius: 8; -fx-border-radius: 8; "
                    + "-fx-border-width: 1;";
    private static final String ESTILO_PANEL_EXITO = ESTILO_PANEL_BASE
            + "-fx-background-color: #F0FDF4; -fx-border-color: #BBF7D0;";
    private static final String ESTILO_PANEL_ERROR = ESTILO_PANEL_BASE
            + "-fx-background-color: #FEF2F2; -fx-border-color: #FECACA;";
    private static final String ESTILO_PANEL_AVISO = ESTILO_PANEL_BASE
            + "-fx-background-color: #FFFBEB; -fx-border-color: #FDE68A;";
    private static final String ESTILO_PANEL_INFO = ESTILO_PANEL_BASE
            + "-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE;";

    // =========================================================================
    // ELEMENTOS FXML
    // =========================================================================
    @FXML private TabPane tabPane;
    @FXML private Label lblEstadoXml;
    @FXML private Label lblResumenGeneral;

    // Pestaña 1: búsqueda y disponibilidad
    @FXML private TextField txtBusquedaNumero;
    @FXML private ComboBox<String> cbxFiltroEstado;
    @FXML private Button btnBuscar;
    @FXML private Label lblResultadoBusqueda;
    @FXML private VBox boxDetalleBusqueda;
    @FXML private Label lblDetalleHabitacion;
    @FXML private Label lblDetalleCliente;
    @FXML private Label lblDetalleFechas;
    @FXML private Label lblDetalleNota;
    @FXML private TableView<Habitacion> tblHabitaciones;
    @FXML private TableColumn<Habitacion, String> colNumero;
    @FXML private TableColumn<Habitacion, String> colTipo;
    @FXML private TableColumn<Habitacion, String> colCapacidad;
    @FXML private TableColumn<Habitacion, String> colPlanta;
    @FXML private TableColumn<Habitacion, String> colEstado;
    @FXML private TableColumn<Habitacion, String> colPrecio;
    @FXML private TableColumn<Habitacion, String> colCliente;
    @FXML private Label lblResumenHabitaciones;

    // Pestaña 2: nueva reserva
    @FXML private TextField txtNumeroReserva;
    @FXML private TextField txtCliente;
    @FXML private TextField txtDocumento;
    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private Label lblInfoHabitacionReserva;
    @FXML private Label lblMensajeReserva;
    @FXML private Label lblResumenNuevaReserva;
    @FXML private Button btnConfirmarReserva;

    // Pestaña 3: panel de recepción
    @FXML private TableView<Reserva> tblReservasActivas;
    @FXML private TableColumn<Reserva, String> colResId;
    @FXML private TableColumn<Reserva, String> colResHabitacion;
    @FXML private TableColumn<Reserva, String> colResCliente;
    @FXML private TableColumn<Reserva, String> colResDocumento;
    @FXML private TableColumn<Reserva, String> colResCheckIn;
    @FXML private TableColumn<Reserva, String> colResCheckOut;
    @FXML private TableColumn<Reserva, String> colResNoches;
    @FXML private TableColumn<Reserva, String> colResEstado;
    @FXML private Button btnCheckIn;
    @FXML private Button btnCheckOut;
    @FXML private Button btnCancelarReserva;
    @FXML private Label lblSeleccionRecepcion;
    @FXML private Label lblMensajeRecepcion;
    @FXML private Label lblResumenRecepcion;

    // Pestaña 4: informes
    @FXML private Label lblKpiTotal;
    @FXML private Label lblKpiDisponibles;
    @FXML private Label lblKpiReservadas;
    @FXML private Label lblKpiOcupadas;
    @FXML private Label lblKpiMantenimiento;
    @FXML private Label lblKpiOcupacion;
    @FXML private TableView<ResumenTipo> tblEstadisticasTipo;
    @FXML private TableColumn<ResumenTipo, String> colEstTipo;
    @FXML private TableColumn<ResumenTipo, Number> colEstTotal;
    @FXML private TableColumn<ResumenTipo, Number> colEstDisponibles;
    @FXML private TableColumn<ResumenTipo, Number> colEstReservadas;
    @FXML private TableColumn<ResumenTipo, Number> colEstOcupadas;
    @FXML private TableColumn<ResumenTipo, String> colEstPrecioMedio;
    @FXML private Label lblResumenEconomico;
    @FXML private Label lblResumenInformes;

    // =========================================================================
    // ESTADO EN MEMORIA
    // =========================================================================
    private final ObservableList<Habitacion> todasLasHabitaciones = FXCollections.observableArrayList();
    private final ObservableList<Habitacion> habitacionesFiltradas = FXCollections.observableArrayList();
    private final ObservableList<Reserva> todasLasReservas = FXCollections.observableArrayList();
    private final ObservableList<Reserva> reservasActivas = FXCollections.observableArrayList();
    private final ObservableList<ResumenTipo> resumenPorTipo = FXCollections.observableArrayList();

    private GestorXml gestor;
    private GestorXml.DatosHotel datosHotel;
    private boolean datosCargados;

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTablas();
        configurarFormulario();
        configurarNavegacion();
        cargarDatos();
    }

    private void configurarNavegacion() {
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, anterior, indice) -> {
            if (indice == null) {
                return;
            }
            if (indice.intValue() == 2) {
                actualizarResumenRecepcion();
                actualizarBotonesRecepcion(tblReservasActivas.getSelectionModel().getSelectedItem());
            } else if (indice.intValue() == 3) {
                actualizarInformes();
            }
        });
    }

    private void configurarFormulario() {
        cbxFiltroEstado.setItems(FXCollections.observableArrayList(
                "Todas", "Disponibles", "Reservadas", "Ocupadas", "Mantenimiento"));
        cbxFiltroEstado.getSelectionModel().select("Todas");

        LocalDate hoy = LocalDate.now();
        dpCheckIn.setValue(hoy);
        dpCheckOut.setValue(hoy.plusDays(2));

        dpCheckIn.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha == null || fecha.isBefore(LocalDate.now()));
            }
        });

        dpCheckOut.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                LocalDate entrada = dpCheckIn.getValue();
                setDisable(vacio || fecha == null || (entrada != null && !fecha.isAfter(entrada)));
            }
        });

        dpCheckIn.valueProperty().addListener((obs, anterior, entrada) -> {
            if (entrada != null && (dpCheckOut.getValue() == null
                    || !dpCheckOut.getValue().isAfter(entrada))) {
                dpCheckOut.setValue(entrada.plusDays(1));
            }
        });

        mostrarMensaje(lblResultadoBusqueda,
                "Introduce un número de habitación o utiliza el filtro de estado.", ESTILO_NEUTRO);
        mostrarMensaje(lblMensajeReserva,
                "Completa los datos y pulsa «Confirmar reserva». La validación se ejecuta antes de escribir en el XML.",
                ESTILO_NEUTRO);
        mostrarMensaje(lblMensajeRecepcion,
                "Selecciona una reserva activa para habilitar las acciones de recepción.", ESTILO_NEUTRO);
        lblInfoHabitacionReserva.setText("Escribe un número de habitación para consultar su disponibilidad.");
        lblSeleccionRecepcion.setText("Ninguna reserva seleccionada.");
        actualizarBotonesRecepcion(null);
    }

    // =========================================================================
    // CONFIGURACIÓN DE TABLAS
    // =========================================================================
    private void configurarTablas() {
        colNumero.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNumeroTexto()));
        colTipo.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTipo()));
        colCapacidad.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getCapacidadTexto()));
        colPlanta.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPlantaTexto()));
        colEstado.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEstadoEtiqueta()));
        colPrecio.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPrecioTexto()));
        colCliente.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getClienteTexto()));

        colEstado.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle(estiloEstadoHabitacion(item));
            }
        });

        tblHabitaciones.setItems(habitacionesFiltradas);

        colResId.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getId()));
        colResHabitacion.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNumeroHabitacionTexto()));
        colResCliente.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getCliente()));
        colResDocumento.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getDocumentoContactoTexto()));
        colResCheckIn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFechaCheckInTexto()));
        colResCheckOut.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFechaCheckOutTexto()));
        colResNoches.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNochesTexto()));
        colResEstado.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEstadoEtiqueta()));

        colResEstado.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle(estiloEstadoReserva(item));
            }
        });

        tblReservasActivas.setItems(reservasActivas);
        tblReservasActivas.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> actualizarBotonesRecepcion(seleccionada));

        colEstTipo.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTipo()));
        colEstTotal.setCellValueFactory(d -> new ReadOnlyIntegerWrapper(d.getValue().getTotal()));
        colEstDisponibles.setCellValueFactory(d -> new ReadOnlyIntegerWrapper(d.getValue().getDisponibles()));
        colEstReservadas.setCellValueFactory(d -> new ReadOnlyIntegerWrapper(d.getValue().getReservadas()));
        colEstOcupadas.setCellValueFactory(d -> new ReadOnlyIntegerWrapper(d.getValue().getOcupadas()));
        colEstPrecioMedio.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPrecioMedioTexto()));

        tblEstadisticasTipo.setItems(resumenPorTipo);
    }

    private String estiloEstadoHabitacion(String estado) {
        return switch (estado) {
            case "Disponible" -> "-fx-text-fill: #15803D; -fx-font-weight: bold;";
            case "Reservada" -> "-fx-text-fill: #1D4ED8; -fx-font-weight: bold;";
            case "Ocupada" -> "-fx-text-fill: #B91C1C; -fx-font-weight: bold;";
            case "En mantenimiento" -> "-fx-text-fill: #B45309; -fx-font-weight: bold;";
            default -> "-fx-text-fill: #334155;";
        };
    }

    private String estiloEstadoReserva(String estado) {
        return switch (estado) {
            case "Confirmada" -> "-fx-text-fill: #15803D; -fx-font-weight: bold;";
            case "Alojada" -> "-fx-text-fill: #1D4ED8; -fx-font-weight: bold;";
            case "Pendiente" -> "-fx-text-fill: #B45309; -fx-font-weight: bold;";
            case "Finalizada" -> "-fx-text-fill: #64748B; -fx-font-weight: bold;";
            case "Cancelada" -> "-fx-text-fill: #B91C1C; -fx-font-weight: bold;";
            default -> "-fx-text-fill: #334155;";
        };
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================
    private void cargarDatos() {
        try {
            if (gestor == null) {
                gestor = new GestorXml();
            }

            datosHotel = gestor.cargar();
            if (datosHotel.habitaciones().isEmpty()) {
                throw new GestorXml.GestorXmlException("El archivo XML no contiene habitaciones.");
            }

            todasLasHabitaciones.setAll(datosHotel.habitaciones());
            todasLasReservas.setAll(datosHotel.reservas());
            datosCargados = true;

            actualizarListaReservasActivas();
            aplicarFiltroEstado();
            actualizarResumenes();
            actualizarInformes();
            seleccionarReserva(null);

            lblEstadoXml.setText("● XML sincronizado · " + todasLasHabitaciones.size()
                    + " habitaciones · " + reservasActivas.size() + " reservas activas");
            lblEstadoXml.setStyle("-fx-text-fill: #BBF7D0; -fx-font-size: 11px; -fx-font-weight: bold;");
            lblResumenGeneral.setText(datosHotel.nombreHotel() + " · " + datosHotel.ciudad()
                    + " · " + datosHotel.estrellas() + " estrellas");
        } catch (Exception ex) {
            datosCargados = false;
            todasLasHabitaciones.clear();
            habitacionesFiltradas.clear();
            todasLasReservas.clear();
            reservasActivas.clear();
            resumenPorTipo.clear();

            lblEstadoXml.setText("● Error de sincronización XML");
            lblEstadoXml.setStyle("-fx-text-fill: #FCA5A5; -fx-font-size: 11px; -fx-font-weight: bold;");
            lblResumenGeneral.setText("Revisa el archivo reservas.xml");
            lblResultadoBusqueda.setText("Error: no se han podido cargar los datos del hotel.");
            lblResultadoBusqueda.setStyle(ESTILO_ERROR);
            lblMensajeReserva.setText("Error: la capa de persistencia no está disponible.");
            lblMensajeReserva.setStyle(ESTILO_ERROR);
            lblMensajeRecepcion.setText("Error: no se han podido cargar las reservas.");
            lblMensajeRecepcion.setStyle(ESTILO_ERROR);
            lblResumenHabitaciones.setText("Sin datos disponibles.");
            lblResumenRecepcion.setText("Sin reservas activas.");
            lblResumenNuevaReserva.setText("Sin datos de disponibilidad.");
            btnConfirmarReserva.setDisable(true);
            btnBuscar.setDisable(true);
            ex.printStackTrace();
        }
    }

    private void actualizarListaReservasActivas() {
        List<Reserva> activas = todasLasReservas.stream()
                .filter(Reserva::estaActiva)
                .sorted(Comparator.comparing(Reserva::getFechaCheckIn,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Reserva::getId))
                .toList();
        reservasActivas.setAll(activas);
        actualizarResumenRecepcion();
    }

    private void actualizarResumenes() {
        if (!datosCargados) {
            return;
        }

        long disponibles = todasLasHabitaciones.stream().filter(Habitacion::estaDisponible).count();
        long reservadas = todasLasHabitaciones.stream().filter(Habitacion::estaReservada).count();
        long ocupadas = todasLasHabitaciones.stream().filter(Habitacion::estaOcupada).count();
        long mantenimiento = todasLasHabitaciones.stream().filter(Habitacion::estaEnMantenimiento).count();

        lblResumenGeneral.setText(datosHotel.nombreHotel() + " · " + datosHotel.ciudad()
                + " · " + datosHotel.estrellas() + " estrellas");
        lblResumenNuevaReserva.setText("Disponibles: " + disponibles
                + "  ·  Reservadas: " + reservadas
                + "  ·  Ocupadas: " + ocupadas
                + "  ·  Mantenimiento: " + mantenimiento);
        lblResumenHabitaciones.setText(habitacionesFiltradas.size()
                + " habitaciones visibles de " + todasLasHabitaciones.size() + ".");
    }

    private void actualizarResumenRecepcion() {
        long confirmadas = todasLasReservas.stream().filter(Reserva::estaConfirmada).count();
        long alojadas = todasLasReservas.stream().filter(Reserva::estaAlojada).count();
        long pendientes = todasLasReservas.stream().filter(Reserva::estaPendiente).count();
        lblResumenRecepcion.setText("Reservas activas: " + reservasActivas.size()
                + "  ·  Confirmadas: " + confirmadas
                + "  ·  Alojadas: " + alojadas
                + "  ·  Pendientes: " + pendientes);
    }

    // =========================================================================
    // PESTAÑA 1: BÚSQUEDA Y DISPONIBILIDAD
    // =========================================================================
    @FXML
    public void buscarHabitacion() {
        if (!datosCargados) {
            mostrarMensaje(lblResultadoBusqueda,
                    "Error: no hay datos de habitaciones cargados.", ESTILO_ERROR);
            return;
        }

        String entrada = txtBusquedaNumero.getText() == null
                ? "" : txtBusquedaNumero.getText().trim();

        if (entrada.isEmpty()) {
            mostrarMensaje(lblResultadoBusqueda,
                    "Error: escribe un número de habitación.", ESTILO_ERROR);
            ocultarDetalle();
            txtBusquedaNumero.requestFocus();
            return;
        }

        if (!entrada.matches("\\d{1,6}")) {
            mostrarMensaje(lblResultadoBusqueda,
                    "Error: introduce un número válido (solo dígitos).", ESTILO_ERROR);
            ocultarDetalle();
            txtBusquedaNumero.requestFocus();
            txtBusquedaNumero.selectAll();
            return;
        }

        int numero = Integer.parseInt(entrada);
        Optional<Habitacion> encontrada = todasLasHabitaciones.stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst();

        if (encontrada.isEmpty()) {
            mostrarMensaje(lblResultadoBusqueda,
                    "Error: la habitación " + numero + " no existe.", ESTILO_ERROR);
            ocultarDetalle();
            habitacionesFiltradas.setAll(todasLasHabitaciones);
            return;
        }

        Habitacion habitacion = encontrada.get();
        habitacionesFiltradas.setAll(List.of(habitacion));
        tblHabitaciones.getSelectionModel().select(habitacion);
        tblHabitaciones.scrollTo(habitacion);
        actualizarResumenes();

        switch (habitacion.getEstado()) {
            case "disponible" -> {
                mostrarMensaje(lblResultadoBusqueda,
                        "Habitación " + numero + " (" + habitacion.getTipo() + "): DISPONIBLE · "
                                + habitacion.getPrecioTexto() + "/noche · "
                                + habitacion.getCapacidadTexto(),
                        ESTILO_EXITO);
                mostrarDetalle(habitacion, ESTILO_PANEL_EXITO);
            }
            case "reservada" -> {
                mostrarMensaje(lblResultadoBusqueda,
                        "Habitación " + numero + " (" + habitacion.getTipo()
                                + "): RESERVADA · pendiente de check-in", ESTILO_INFO);
                mostrarDetalle(habitacion, ESTILO_PANEL_INFO);
            }
            case "ocupada" -> {
                mostrarMensaje(lblResultadoBusqueda,
                        "Habitación " + numero + " (" + habitacion.getTipo()
                                + "): OCUPADA · " + habitacion.getClienteTexto(), ESTILO_ERROR);
                mostrarDetalle(habitacion, ESTILO_PANEL_ERROR);
            }
            case "mantenimiento" -> {
                mostrarMensaje(lblResultadoBusqueda,
                        "Habitación " + numero + " (" + habitacion.getTipo()
                                + "): EN MANTENIMIENTO", ESTILO_AVISO);
                mostrarDetalle(habitacion, ESTILO_PANEL_AVISO);
            }
            default -> {
                mostrarMensaje(lblResultadoBusqueda,
                        "Habitación " + numero + ": estado desconocido.", ESTILO_NEUTRO);
                ocultarDetalle();
            }
        }
    }

    private void mostrarDetalle(Habitacion habitacion, String estiloPanel) {
        boxDetalleBusqueda.setVisible(true);
        boxDetalleBusqueda.setManaged(true);
        boxDetalleBusqueda.setStyle(estiloPanel);

        lblDetalleHabitacion.setText("Habitación " + habitacion.getNumero()
                + " · " + habitacion.getTipo() + " · " + habitacion.getPlantaTexto()
                + " · " + habitacion.getCapacidadTexto());
        lblDetalleCliente.setText("Huésped: " + habitacion.getClienteTexto()
                + "  ·  Contacto: " + habitacion.getDocumentoContactoTexto());
        lblDetalleFechas.setText("Estancia: " + habitacion.getPeriodoTexto()
                + (habitacion.getReservaId().isBlank() ? ""
                : "  ·  " + habitacion.getReservaId()
                + " (" + habitacion.getEstadoReservaTexto() + ")"));
        lblDetalleNota.setText("Observaciones: " + habitacion.getNotaTexto());
    }

    private void ocultarDetalle() {
        boxDetalleBusqueda.setVisible(false);
        boxDetalleBusqueda.setManaged(false);
    }

    @FXML
    public void limpiarBusqueda() {
        txtBusquedaNumero.clear();
        if (cbxFiltroEstado != null) {
            cbxFiltroEstado.getSelectionModel().select("Todas");
        }
        aplicarFiltroEstado();
        ocultarDetalle();
        mostrarMensaje(lblResultadoBusqueda,
                "Introduce un número de habitación o utiliza el filtro de estado.", ESTILO_NEUTRO);
        txtBusquedaNumero.requestFocus();
    }

    @FXML
    public void filtrarPorEstado() {
        aplicarFiltroEstado();
        ocultarDetalle();
        mostrarMensaje(lblResultadoBusqueda,
                "Filtro aplicado: " + cbxFiltroEstado.getValue() + ".", ESTILO_NEUTRO);
    }

    private void aplicarFiltroEstado() {
        if (!datosCargados) {
            return;
        }

        String filtro = cbxFiltroEstado.getValue() == null ? "Todas" : cbxFiltroEstado.getValue();
        List<Habitacion> filtradas = switch (filtro) {
            case "Disponibles" -> todasLasHabitaciones.stream()
                    .filter(Habitacion::estaDisponible).toList();
            case "Reservadas" -> todasLasHabitaciones.stream()
                    .filter(Habitacion::estaReservada).toList();
            case "Ocupadas" -> todasLasHabitaciones.stream()
                    .filter(Habitacion::estaOcupada).toList();
            case "Mantenimiento" -> todasLasHabitaciones.stream()
                    .filter(Habitacion::estaEnMantenimiento).toList();
            default -> List.copyOf(todasLasHabitaciones);
        };

        habitacionesFiltradas.setAll(filtradas);
        actualizarResumenes();
    }

    // =========================================================================
    // PESTAÑA 2: NUEVA RESERVA
    // =========================================================================
    @FXML
    public void consultarHabitacionReserva() {
        if (!datosCargados) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: no hay datos cargados.", ESTILO_ERROR);
            return;
        }

        String texto = txtNumeroReserva.getText() == null
                ? "" : txtNumeroReserva.getText().trim();
        if (!texto.matches("\\d{1,6}")) {
            lblInfoHabitacionReserva.setText("Introduce un número de habitación válido.");
            lblInfoHabitacionReserva.setStyle("-fx-text-fill: #B91C1C; -fx-font-size: 12px;");
            return;
        }

        Optional<Habitacion> habitacion = todasLasHabitaciones.stream()
                .filter(h -> h.getNumero() == Integer.parseInt(texto))
                .findFirst();

        if (habitacion.isEmpty()) {
            lblInfoHabitacionReserva.setText("La habitación " + texto + " no existe.");
            lblInfoHabitacionReserva.setStyle("-fx-text-fill: #B91C1C; -fx-font-size: 12px;");
            return;
        }

        Habitacion h = habitacion.get();
        String estado = h.getEstadoEtiqueta();
        String color = h.estaDisponible() ? "#166534"
                : h.estaReservada() ? "#1D4ED8"
                : h.estaOcupada() ? "#B91C1C" : "#B45309";
        lblInfoHabitacionReserva.setText("Habitación " + h.getNumero() + " · " + h.getTipo()
                + " · " + h.getPrecioTexto() + "/noche · Estado: " + estado);
        lblInfoHabitacionReserva.setStyle("-fx-text-fill: " + color
                + "; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    @FXML
    public void confirmarReserva() {
        if (!datosCargados || gestor == null) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: la capa de persistencia no está disponible.", ESTILO_ERROR);
            return;
        }

        String numeroTexto = txtNumeroReserva.getText() == null
                ? "" : txtNumeroReserva.getText().trim();
        if (!numeroTexto.matches("\\d{1,6}")) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: introduce un número de habitación válido.", ESTILO_ERROR);
            txtNumeroReserva.requestFocus();
            return;
        }

        String cliente = txtCliente.getText() == null ? "" : txtCliente.getText().trim();
        String documento = txtDocumento.getText() == null ? "" : txtDocumento.getText().trim();
        LocalDate checkIn = dpCheckIn.getValue();
        LocalDate checkOut = dpCheckOut.getValue();

        if (cliente.length() < 3) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: el nombre del cliente debe tener al menos 3 caracteres.", ESTILO_ERROR);
            txtCliente.requestFocus();
            return;
        }
        if (documento.length() < 5) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: indica un DNI o teléfono de contacto válido.", ESTILO_ERROR);
            txtDocumento.requestFocus();
            return;
        }
        if (checkIn == null || checkOut == null) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: selecciona las fechas de entrada y salida.", ESTILO_ERROR);
            return;
        }
        if (!checkOut.isAfter(checkIn)) {
            mostrarMensaje(lblMensajeReserva,
                    "Error: la fecha de salida debe ser posterior a la de entrada.", ESTILO_ERROR);
            return;
        }

        int numero = Integer.parseInt(numeroTexto);
        btnConfirmarReserva.setDisable(true);

        try {
            Reserva reserva = gestor.crearReserva(numero, cliente, documento, checkIn, checkOut);
            cargarDatos();
            seleccionarReserva(reserva.getId());
            mostrarMensaje(lblMensajeReserva,
                    "Reserva " + reserva.getId() + " confirmada · Habitación "
                            + reserva.getNumeroHabitacion() + " · " + reserva.getPeriodoTexto()
                            + " · Importe estimado: " + reserva.getImporteEstimadoTexto()
                            + ". XML actualizado.",
                    ESTILO_EXITO);
            lblInfoHabitacionReserva.setText("Reserva guardada en reservas.xml. La habitación queda en estado RESERVADA.");
            lblInfoHabitacionReserva.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px; -fx-font-weight: bold;");

            txtNumeroReserva.clear();
            txtCliente.clear();
            txtDocumento.clear();
            txtNumeroReserva.requestFocus();
        } catch (GestorXml.GestorXmlException ex) {
            mostrarMensaje(lblMensajeReserva, "Error: " + ex.getMessage(), ESTILO_ERROR);
        } catch (RuntimeException ex) {
            mostrarMensaje(lblMensajeReserva,
                    "Error inesperado al guardar la reserva: " + ex.getMessage(), ESTILO_ERROR);
        } finally {
            btnConfirmarReserva.setDisable(false);
        }
    }

    // =========================================================================
    // PESTAÑA 3: RECEPCIÓN (CHECK-IN / CHECK-OUT)
    // =========================================================================
    @FXML
    public void realizarCheckIn() {
        Reserva seleccionada = tblReservasActivas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error: selecciona una reserva de la tabla.", ESTILO_ERROR);
            return;
        }
        if (gestor == null || !datosCargados) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error: la capa de persistencia no está disponible.", ESTILO_ERROR);
            return;
        }

        try {
            Reserva reserva = gestor.realizarCheckIn(seleccionada.getId());
            cargarDatos();
            seleccionarReserva(reserva.getId());
            mostrarMensaje(lblMensajeRecepcion,
                    "Check-in completado · " + reserva.getId() + " · Habitación "
                            + reserva.getNumeroHabitacion() + " ahora OCUPADA · XML actualizado.",
                    ESTILO_EXITO);
        } catch (GestorXml.GestorXmlException ex) {
            mostrarMensaje(lblMensajeRecepcion, "Error: " + ex.getMessage(), ESTILO_ERROR);
        } catch (RuntimeException ex) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error inesperado en el check-in: " + ex.getMessage(), ESTILO_ERROR);
        }
    }

    @FXML
    public void realizarCheckOut() {
        Reserva seleccionada = tblReservasActivas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error: selecciona una reserva de la tabla.", ESTILO_ERROR);
            return;
        }
        if (gestor == null || !datosCargados) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error: la capa de persistencia no está disponible.", ESTILO_ERROR);
            return;
        }

        try {
            Reserva reserva = gestor.realizarCheckOut(seleccionada.getId());
            int numeroHabitacion = reserva.getNumeroHabitacion();
            cargarDatos();
            mostrarMensaje(lblMensajeRecepcion,
                    "Check-out completado · " + reserva.getId() + " · Habitación "
                            + numeroHabitacion + " liberada a DISPONIBLE · XML actualizado.",
                    ESTILO_EXITO);
        } catch (GestorXml.GestorXmlException ex) {
            mostrarMensaje(lblMensajeRecepcion, "Error: " + ex.getMessage(), ESTILO_ERROR);
        } catch (RuntimeException ex) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error inesperado en el check-out: " + ex.getMessage(), ESTILO_ERROR);
        }
    }

    @FXML
    public void cancelarReserva() {
        Reserva seleccionada = tblReservasActivas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error: selecciona una reserva de la tabla.", ESTILO_ERROR);
            return;
        }
        if (gestor == null || !datosCargados) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error: la capa de persistencia no está disponible.", ESTILO_ERROR);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar reserva");
        confirmacion.setHeaderText("Cancelar " + seleccionada.getId());
        confirmacion.setContentText("Se liberará la habitación " + seleccionada.getNumeroHabitacion()
                + " y la reserva quedará cancelada en reservas.xml.");
        if (tblReservasActivas.getScene() != null
                && tblReservasActivas.getScene().getWindow() instanceof Stage stage) {
            confirmacion.initOwner(stage);
        }

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            Reserva reserva = gestor.cancelarReserva(seleccionada.getId());
            cargarDatos();
            mostrarMensaje(lblMensajeRecepcion,
                    "Reserva " + reserva.getId() + " cancelada · Habitación "
                            + reserva.getNumeroHabitacion() + " disponible · XML actualizado.",
                    ESTILO_EXITO);
        } catch (GestorXml.GestorXmlException ex) {
            mostrarMensaje(lblMensajeRecepcion, "Error: " + ex.getMessage(), ESTILO_ERROR);
        } catch (RuntimeException ex) {
            mostrarMensaje(lblMensajeRecepcion,
                    "Error inesperado al cancelar: " + ex.getMessage(), ESTILO_ERROR);
        }
    }

    private void actualizarBotonesRecepcion(Reserva seleccionada) {
        boolean haySeleccion = seleccionada != null;

        boolean puedeCheckIn = false;
        if (haySeleccion && seleccionada.estaConfirmada() && datosCargados) {
            Optional<Habitacion> habitacion = todasLasHabitaciones.stream()
                    .filter(h -> h.getNumero() == seleccionada.getNumeroHabitacion())
                    .findFirst();
            puedeCheckIn = habitacion.isPresent()
                    && !habitacion.get().estaOcupada()
                    && !habitacion.get().estaEnMantenimiento();
        }

        boolean puedeCheckOut = haySeleccion && seleccionada.estaAlojada();
        boolean puedeCancelar = haySeleccion
                && (seleccionada.estaConfirmada() || seleccionada.estaPendiente());

        btnCheckIn.setDisable(!puedeCheckIn);
        btnCheckOut.setDisable(!puedeCheckOut);
        btnCancelarReserva.setDisable(!puedeCancelar);

        if (!haySeleccion) {
            lblSeleccionRecepcion.setText("Ninguna reserva seleccionada.");
            return;
        }

        lblSeleccionRecepcion.setText(seleccionada.getId() + " · Habitación "
                + seleccionada.getNumeroHabitacion() + " · " + seleccionada.getCliente()
                + " · " + seleccionada.getPeriodoTexto() + " · " + seleccionada.getEstadoEtiqueta());
    }

    private void seleccionarReserva(String id) {
        if (id == null) {
            tblReservasActivas.getSelectionModel().clearSelection();
            actualizarBotonesRecepcion(null);
            return;
        }

        reservasActivas.stream()
                .filter(r -> r.getId().equalsIgnoreCase(id))
                .findFirst()
                .ifPresent(r -> {
                    tblReservasActivas.getSelectionModel().select(r);
                    tblReservasActivas.scrollTo(r);
                    actualizarBotonesRecepcion(r);
                });
    }

    // =========================================================================
    // PESTAÑA 4: INFORMES
    // =========================================================================
    private void actualizarInformes() {
        if (!datosCargados || todasLasHabitaciones.isEmpty()) {
            return;
        }

        int total = todasLasHabitaciones.size();
        int disponibles = (int) todasLasHabitaciones.stream().filter(Habitacion::estaDisponible).count();
        int reservadas = (int) todasLasHabitaciones.stream().filter(Habitacion::estaReservada).count();
        int ocupadas = (int) todasLasHabitaciones.stream().filter(Habitacion::estaOcupada).count();
        int mantenimiento = (int) todasLasHabitaciones.stream().filter(Habitacion::estaEnMantenimiento).count();
        double porcentajeOcupacion = total > 0 ? (ocupadas * 100.0) / total : 0.0;

        lblKpiTotal.setText(String.valueOf(total));
        lblKpiDisponibles.setText(String.valueOf(disponibles));
        lblKpiReservadas.setText(String.valueOf(reservadas));
        lblKpiOcupadas.setText(String.valueOf(ocupadas));
        lblKpiMantenimiento.setText(String.valueOf(mantenimiento));
        lblKpiOcupacion.setText(String.format(LOCALE_ES, "%.1f%%", porcentajeOcupacion));

        Map<String, List<Habitacion>> porTipo = new LinkedHashMap<>();
        for (Habitacion habitacion : todasLasHabitaciones) {
            porTipo.computeIfAbsent(habitacion.getTipo(), clave -> new ArrayList<>()).add(habitacion);
        }

        List<ResumenTipo> resumenes = new ArrayList<>();
        double ingresosDiarios = 0.0;

        for (Map.Entry<String, List<Habitacion>> entrada : porTipo.entrySet()) {
            List<Habitacion> listaTipo = entrada.getValue();
            int totalTipo = listaTipo.size();
            int disponiblesTipo = (int) listaTipo.stream().filter(Habitacion::estaDisponible).count();
            int reservadasTipo = (int) listaTipo.stream().filter(Habitacion::estaReservada).count();
            int ocupadasTipo = (int) listaTipo.stream().filter(Habitacion::estaOcupada).count();
            double precioMedio = listaTipo.stream().mapToDouble(Habitacion::getPrecio).average().orElse(0);

            resumenes.add(new ResumenTipo(entrada.getKey(), totalTipo, disponiblesTipo,
                    reservadasTipo, ocupadasTipo, precioMedio));

            ingresosDiarios += listaTipo.stream()
                    .filter(Habitacion::estaOcupada)
                    .mapToDouble(Habitacion::getPrecio)
                    .sum();
        }

        resumenPorTipo.setAll(resumenes);

        lblResumenEconomico.setText(String.format(LOCALE_ES,
                "Facturación estimada: %.2f €/noche por %d habitaciones ocupadas · "
                        + "%d reservas confirmadas en cartera.",
                ingresosDiarios, ocupadas, reservadas));
        lblResumenInformes.setText("Métricas calculadas en tiempo real desde reservas.xml · "
                + LocalDate.now().format(FORMATO_FECHA));
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================
    private void mostrarMensaje(Label etiqueta, String texto, String estilo) {
        etiqueta.setText(texto);
        etiqueta.setStyle(estilo);
    }

    /**
     * Resumen agregado por tipo de habitación para la pestaña de informes.
     */
    public static class ResumenTipo {
        private final String tipo;
        private final int total;
        private final int disponibles;
        private final int reservadas;
        private final int ocupadas;
        private final double precioMedio;

        public ResumenTipo(String tipo, int total, int disponibles, int reservadas,
                           int ocupadas, double precioMedio) {
            this.tipo = tipo;
            this.total = total;
            this.disponibles = disponibles;
            this.reservadas = reservadas;
            this.ocupadas = ocupadas;
            this.precioMedio = precioMedio;
        }

        public String getTipo() {
            return tipo;
        }

        public int getTotal() {
            return total;
        }

        public int getDisponibles() {
            return disponibles;
        }

        public int getReservadas() {
            return reservadas;
        }

        public int getOcupadas() {
            return ocupadas;
        }

        public double getPrecioMedio() {
            return precioMedio;
        }

        public String getPrecioMedioTexto() {
            return String.format(LOCALE_ES, "%.2f €", precioMedio);
        }
    }
}
