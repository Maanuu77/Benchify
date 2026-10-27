package com.benchify.checkin.application.port.in;

import com.benchify.checkin.domain.CheckIn;

/** Check-in con el título del banco; el título falta si el banco ya no existe. */
public record CheckInConBanco(CheckIn checkIn, String tituloBanco) {
}
