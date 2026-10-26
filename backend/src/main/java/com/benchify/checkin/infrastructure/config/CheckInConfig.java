package com.benchify.checkin.infrastructure.config;

import com.benchify.checkin.domain.PoliticaCheckIn;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(CheckInConfig.CheckInProperties.class)
class CheckInConfig {

    @Bean
    PoliticaCheckIn politicaCheckIn(CheckInProperties properties) {
        return new PoliticaCheckIn(properties.distanciaMaximaMetros(), properties.intervaloMinimo());
    }

    /** {@code benchify.checkin.*}: el GPS de un móvil en ciudad suele fallar unas decenas de metros. */
    @ConfigurationProperties(prefix = "benchify.checkin")
    record CheckInProperties(
            @DefaultValue("150") double distanciaMaximaMetros,
            @DefaultValue("12h") Duration intervaloMinimo) {
    }
}
