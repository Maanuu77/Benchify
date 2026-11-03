package com.benchify.banco.infrastructure.storage;

import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiControllerTest(FicherosPublicosRouter.class)
@TestPropertySource(properties = "benchify.almacen.tipo=postgres")
class FicherosPublicosRouterTest {

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private PostgresAlmacenImagenes almacen;

    @Test
    void servesApprovedPhotosToAnyoneWithLongCaching() {
        when(almacen.leer("bancos/b1/f.jpg")).thenReturn(Optional.of(new byte[]{1, 2}));

        assertThat(mvc.get().uri("/api/v1/ficheros/bancos/b1/f.jpg"))
                .hasStatusOk()
                .hasContentType("image/jpeg")
                .hasHeader("Cache-Control", "max-age=31536000, public, immutable");
        assertThat(mvc.get().uri("/api/v1/ficheros/bancos/b1/no-existe.jpg")).hasStatus(404);
    }

    @Test
    void neverServesPendingPhotos() {
        assertThat(mvc.get().uri("/api/v1/ficheros/pendientes/b1/f.jpg")).hasStatus(404);
        // Spring Security ya rechaza las rutas con ".." antes de llegar al almacén
        assertThat(mvc.get().uri("/api/v1/ficheros/bancos/../pendientes/b1/f.jpg")).hasStatus4xxClientError();
        verify(almacen, never()).leer(anyString());
    }
}
