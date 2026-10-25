package com.benchify.shared.infrastructure.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un controlador el {@link com.benchify.shared.domain.Actor} autenticado.
 * Con {@code required = false} recibe {@code null} si la petición es anónima.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentActor {

    boolean required() default true;
}
