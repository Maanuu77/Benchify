package com.benchify.moderacion.application.service;

import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.application.port.out.ImagenRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeracionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Actor ADMIN = Actor.admin("root");

    @Mock
    private BancoRepository bancos;
    @Mock
    private ImagenRepository imagenes;
    @Mock
    private ImagenesBancoUseCase imagenesBanco;
    @Mock
    private ResenaRepository resenaRepository;
    @Mock
    private ResenasUseCase resenas;
    @Mock
    private UsuarioRepository usuarios;

    private ModeracionService service;

    @BeforeEach
    void setUp() {
        service = new ModeracionService(bancos, imagenes, imagenesBanco, resenaRepository, resenas, usuarios,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        lenient().when(bancos.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "ana")
    void everyOperationRequiresAnAdmin(String usuario) {
        Actor noAdmin = usuario == null ? null : Actor.user(usuario);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.estadisticas(noAdmin)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.aprobarBanco(noAdmin, id)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.eliminarResena(noAdmin, id)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.rechazarImagen(noAdmin, id)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(bancos, imagenes, resenas, imagenesBanco);
    }

    @Test
    void approvesAndRejectsBenches() {
        Banco banco = Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Nuevo", null, new Ubicacion(37, -6), null, null, null), Actor.user("ana"), AHORA);
        when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));

        assertThat(service.aprobarBanco(ADMIN, banco.id()).estado()).isEqualTo(EstadoBanco.PUBLICADO);
        assertThat(banco.fechaVerificacion()).isEqualTo(AHORA);
        assertThat(service.rechazarBanco(ADMIN, banco.id()).estado()).isEqualTo(EstadoBanco.OCULTO);
    }

    @Test
    void unknownBenchIs404() {
        UUID id = UUID.randomUUID();
        when(bancos.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprobarBanco(ADMIN, id)).isInstanceOf(BancoNotFoundException.class);
    }

    @Test
    void reviewDecisionsDelegateToTheReviewFeature() {
        UUID resena = UUID.randomUUID();

        service.eliminarResena(ADMIN, resena);
        service.mantenerResena(ADMIN, resena);

        verify(resenas).eliminar(ADMIN, resena);
        verify(resenas).descartarReportes(ADMIN, resena);
    }

    @Test
    void imageDecisionsDelegateToTheBenchFeature() {
        Imagen aprobada = imagen().aprobar();
        when(imagenesBanco.aprobar(ADMIN, aprobada.id())).thenReturn(aprobada);

        assertThat(service.aprobarImagen(ADMIN, aprobada.id())).isEqualTo(aprobada);
        service.rechazarImagen(ADMIN, aprobada.id());

        verify(imagenesBanco).rechazar(ADMIN, aprobada.id());
    }

    @Test
    void aggregatesStatistics() {
        when(bancos.countByEstado(EstadoBanco.PENDIENTE)).thenReturn(3L);
        when(bancos.countByEstado(EstadoBanco.PUBLICADO)).thenReturn(40L);
        when(bancos.countByEstado(EstadoBanco.OCULTO)).thenReturn(2L);
        when(resenaRepository.countReportadas()).thenReturn(1L);
        when(imagenes.countByEstado(EstadoImagen.PENDIENTE)).thenReturn(5L);
        when(resenaRepository.count()).thenReturn(120L);
        when(usuarios.count()).thenReturn(33L);

        EstadisticasModeracion estadisticas = service.estadisticas(ADMIN);

        assertThat(estadisticas).isEqualTo(new EstadisticasModeracion(3, 40, 2, 1, 5, 120, 33));
        assertThat(estadisticas.tareasPendientes()).isEqualTo(9);
    }

    @Test
    void listsQueues() {
        PageQuery page = PageQuery.first(10);
        when(bancos.findByEstado(EstadoBanco.PENDIENTE, page)).thenReturn(PageResult.empty(page));
        when(resenaRepository.findReportadas(page)).thenReturn(PageResult.empty(page));
        when(imagenes.findByEstado(EstadoImagen.PENDIENTE, page)).thenReturn(PageResult.empty(page));

        assertThat(service.bancos(ADMIN, EstadoBanco.PENDIENTE, page).content()).isEmpty();
        assertThat(service.resenasReportadas(ADMIN, page).content()).isEmpty();
        assertThat(service.imagenesPendientes(ADMIN, page).content()).isEmpty();
    }

    private static Imagen imagen() {
        UUID id = UUID.randomUUID();
        return new Imagen(id, UUID.randomUUID(), "bancos/x/" + id + ".jpg", "https://storage/" + id, "image/jpeg",
                10, "ana", EstadoImagen.PENDIENTE, AHORA);
    }
}
