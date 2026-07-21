package com.example.medicalsearch.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    void calculatesTotalPages() {
        PageResponse page = PageResponse.of(0, 20, 41);

        assertThat(page.totalPages()).isEqualTo(3);
    }
}
