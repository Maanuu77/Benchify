package com.benchify.lugar.infrastructure.nominatim;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(GeocodificacionProperties.class)
class GeocodificacionConfig {

    @Bean
    NominatimGeocodificador nominatimGeocodificador(GeocodificacionProperties properties) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory peticiones = new JdkClientHttpRequestFactory(cliente);
        peticiones.setReadTimeout(properties.timeout());
        return new NominatimGeocodificador(RestClient.builder().requestFactory(peticiones), properties);
    }
}
