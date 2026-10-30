package com.hotelapp;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Reserva de una habitación.
 *
 * <p>Una reserva relaciona un huésped con una habitación y un intervalo de
 * estancia. El estado de la reserva es independiente del estado físico de la
 * habitación, aunque ambos se mantienen sincronizados por {@link GestorXml}:</p>
 *
 * <ul>
 *   <li>{@code pendiente}: reserva registrada, pendiente de confirmación.</li>
 *   <li>{@code confirmada}: reserva confirmada, pendiente de check-in.</li>
 *   <li>{@code alojada}: check-in realizado; el huésped está en el hotel.</li>
 *   <li>{@code finalizada}: check-out realizado.</li>
 *   <li>{@code cancelada}: reserva anulada.</li>
 * </ul>
 */
public final class Reserva {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String id;
    private final int numeroHabitacion;
    private final String cliente;
    private final String documentoContacto;
    private final LocalDate fechaCheckIn;
    private final LocalDate fechaCheckOut;
    private final String estado;
    private final double precioNoche;
    private final LocalDate creadaEn;
    private final String nota;

    public Reserva(String id, int numeroHabitacion, String cliente, String documentoContacto,
                   LocalDate fechaCheckIn, LocalDate fechaCheckOut, String estado,
                   double precioNoche, LocalDate creadaEn, String nota) {
        this.id = id == null ? "" : id.trim();
        this.numeroHabitacion = numeroHabitacion;
        this.cliente = cliente == null ? "" : cliente.trim();
        this.documentoContacto = documentoContacto == null ? "" : documentoContacto.trim();
        this.fechaCheckIn = fechaCheckIn;
        this.fechaCheckOut = fechaCheckOut;
        this.estado = normalizarEstado(estado);
        this.precioNoche = Math.max(precioNoche, 0);
        this.creadaEn = creadaEn;
        this.nota = nota == null ? "" : nota.trim();
    }

    /**
     * Constructor de compatibilidad para reservas sin precio/creación.
     */
    public Reserva(String id, int numeroHabitacion, String cliente, String documentoContacto,
                   LocalDate fechaCheckIn, LocalDate fechaCheckOut, String estado, String nota) {
        this(id, numeroHabitacion, cliente, documentoContacto, fechaCheckIn, fechaCheckOut,
                estado, 0, null, nota);
    }

    public static String normalizarEstado(String valor) {
        if (valor == null) {
            return "";
        }

        String limpio = Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return switch (limpio) {
            case "pendiente", "pending" -> "pendiente";
            case "confirmada", "confirmado", "confirmed" -> "confirmada";
            case "alojada", "alojado", "checkin", "check-in", "en curso" -> "alojada";
            case "finalizada", "finalizado", "checkout", "check-out" -> "finalizada";
            case "cancelada", "cancelado", "cancelled" -> "cancelada";
            default -> "";
        };
    }

    public static String etiquetaEstado(String estado) {
        return switch (estado == null ? "" : estado) {
            case "pendiente" -> "Pendiente";
            case "confirmada" -> "Confirmada";
            case "alojada" -> "Alojada";
            case "finalizada" -> "Finalizada";
            case "cancelada" -> "Cancelada";
            default -> "Sin estado";
        };
    }

    public String getId() {
        return id;
    }

    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }

    public String getNumeroHabitacionTexto() {
        return String.valueOf(numeroHabitacion);
    }

    public String getCliente() {
        return cliente;
    }

    public String getDocumentoContacto() {
        return documentoContacto;
    }

    public String getDocumentoContactoTexto() {
        return documentoContacto == null || documentoContacto.isBlank() ? "—" : documentoContacto;
    }

    public LocalDate getFechaCheckIn() {
        return fechaCheckIn;
    }

    public LocalDate getFechaCheckOut() {
        return fechaCheckOut;
    }

    public String getFechaCheckInTexto() {
        return fechaCheckIn == null ? "—" : fechaCheckIn.format(FORMATO_FECHA);
    }

    public String getFechaCheckOutTexto() {
        return fechaCheckOut == null ? "—" : fechaCheckOut.format(FORMATO_FECHA);
    }

    public String getPeriodoTexto() {
        return getFechaCheckInTexto() + "  →  " + getFechaCheckOutTexto();
    }

    public String getEstado() {
        return estado;
    }

    public String getEstadoEtiqueta() {
        return etiquetaEstado(estado);
    }

    public boolean estaPendiente() {
        return "pendiente".equals(estado);
    }

    public boolean estaConfirmada() {
        return "confirmada".equals(estado);
    }

    public boolean estaAlojada() {
        return "alojada".equals(estado);
    }

    public boolean estaFinalizada() {
        return "finalizada".equals(estado);
    }

    public boolean estaCancelada() {
        return "cancelada".equals(estado);
    }

    public boolean estaActiva() {
        return estaPendiente() || estaConfirmada() || estaAlojada();
    }

    public double getPrecioNoche() {
        return precioNoche;
    }

    public String getPrecioNocheTexto() {
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f €", precioNoche);
    }

    public long getNoches() {
        if (fechaCheckIn == null || fechaCheckOut == null || !fechaCheckOut.isAfter(fechaCheckIn)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(fechaCheckIn, fechaCheckOut);
    }

    public String getNochesTexto() {
        long noches = getNoches();
        return noches == 1 ? "1 noche" : noches + " noches";
    }

    public double getImporteEstimado() {
        return getNoches() * precioNoche;
    }

    public String getImporteEstimadoTexto() {
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f €", getImporteEstimado());
    }

    public LocalDate getCreadaEn() {
        return creadaEn;
    }

    public String getCreadaEnTexto() {
        return creadaEn == null ? "—" : creadaEn.format(FORMATO_FECHA);
    }

    public String getNota() {
        return nota;
    }

    @Override
    public String toString() {
        return id + " · Habitación " + numeroHabitacion + " · " + cliente
                + " · " + getEstadoEtiqueta();
    }
}
