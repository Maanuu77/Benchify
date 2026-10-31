package com.benchify.shared.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorTest {

    @Test
    void ownerAndAdminCanManageAResource() {
        assertThat(Actor.user("ana").canManage("ana")).isTrue();
        assertThat(Actor.user("ana").canManage("luis")).isFalse();
        assertThat(Actor.admin("root").canManage("luis")).isTrue();
    }

    @Test
    void readsAdminRoleFromClaimIgnoringCase() {
        assertThat(Rol.fromClaim("ADMIN")).isEqualTo(Rol.ADMIN);
        assertThat(Rol.fromClaim(" admin ")).isEqualTo(Rol.ADMIN);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "superuser", "moderador"})
    void unknownClaimsFallBackToUser(String claim) {
        assertThat(Rol.fromClaim(claim)).isEqualTo(Rol.USER);
    }

    @Test
    void nonStringClaimFallsBackToUser() {
        assertThat(Rol.fromClaim(Boolean.TRUE)).isEqualTo(Rol.USER);
    }

    @Test
    void publishingRequiresAVerifiedEmailExceptForAdmins() {
        assertThatThrownBy(() -> new Actor("ana", Rol.USER, false).exigirEmailVerificado())
                .isInstanceOfSatisfying(ForbiddenException.class, e -> assertThat(e.code()).isEqualTo("EMAIL_NO_VERIFICADO"));
        assertThatCode(() -> new Actor("ana", Rol.USER, true).exigirEmailVerificado()).doesNotThrowAnyException();
        assertThatCode(() -> new Actor("admin", Rol.ADMIN, false).exigirEmailVerificado()).doesNotThrowAnyException();
        assertThat(Actor.user("ana").emailVerificado()).isTrue();
    }
}
