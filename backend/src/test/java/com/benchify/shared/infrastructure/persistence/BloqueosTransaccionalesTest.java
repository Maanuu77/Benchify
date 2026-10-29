package com.benchify.shared.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BloqueosTransaccionalesTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    @AfterEach
    void sinTransaccion() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void takesATransactionScopedAdvisoryLockForTheKey() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        BloqueosTransaccionales.bloquear(jdbc, "checkin:ana:42");

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).execute(eq("SELECT pg_advisory_xact_lock(hashtextextended(:clave, 0))"), params.capture(),
                any(PreparedStatementCallback.class));
        assertThat(params.getValue().getValue("clave")).isEqualTo("checkin:ana:42");
    }

    @Test
    void refusesToLockOutsideATransactionBecauseTheLockWouldBeReleasedAtOnce() {
        assertThatThrownBy(() -> BloqueosTransaccionales.bloquear(jdbc, "checkin:ana:42"))
                .isInstanceOf(IllegalStateException.class);
        verify(jdbc, never()).execute(anyString(), any(MapSqlParameterSource.class), any(PreparedStatementCallback.class));
    }
}
