package com.benchify.moderacion.domain;

/** Resumen del panel de administración: trabajo pendiente y volumen de contenido. */
public record EstadisticasModeracion(
        long bancosPendientes,
        long bancosPublicados,
        long bancosOcultos,
        long resenasReportadas,
        long imagenesPendientes,
        long totalResenas,
        long totalUsuarios) {

    public long tareasPendientes() {
        return bancosPendientes + resenasReportadas + imagenesPendientes;
    }
}
