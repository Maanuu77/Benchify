package com.benchify.shared.infrastructure.web;

import com.benchify.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void healthAndProbesArePublicAndHideDetails() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.status").isEqualTo("UP");
                    assertThat(json).doesNotHavePath("$.components");
                });
        assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
        assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk();
    }

    @Test
    void otherActuatorEndpointsAreNotPublic() {
        assertThat(mvc.get().uri("/actuator/info")).hasStatus(401);
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(401);
    }
}
