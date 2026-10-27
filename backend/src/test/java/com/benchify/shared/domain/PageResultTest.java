package com.benchify.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PageResultTest {

    @Test
    void computesTotalPagesAndNextPage() {
        PageResult<String> page = new PageResult<>(List.of("a", "b"), 0, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(new PageResult<>(List.of("e"), 2, 2, 5).hasNext()).isFalse();
    }

    @Test
    void mapsContentKeepingPaginationMetadata() {
        PageResult<Integer> lengths = new PageResult<>(List.of("uno", "dos!"), 1, 2, 4).map(String::length);

        assertThat(lengths.content()).containsExactly(3, 4);
        assertThat(lengths.page()).isEqualTo(1);
        assertThat(lengths.totalElements()).isEqualTo(4);
    }

    @Test
    void emptyPageHasNoPages() {
        PageResult<Object> empty = PageResult.empty(PageQuery.first(10));

        assertThat(empty.content()).isEmpty();
        assertThat(empty.totalPages()).isZero();
    }

    @Test
    void pageQueryRejectsOutOfRangeValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PageQuery(-1, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageQuery(0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1));
        assertThat(new PageQuery(3, 20).offset()).isEqualTo(60);
    }
}
