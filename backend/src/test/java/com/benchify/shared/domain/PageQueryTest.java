package com.benchify.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    void pageAndSizeAreBounded() {
        assertThat(new PageQuery(PageQuery.MAX_PAGE, PageQuery.MAX_SIZE).offset()).isEqualTo(1_000_000L);
        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(PageQuery.MAX_PAGE + 1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
