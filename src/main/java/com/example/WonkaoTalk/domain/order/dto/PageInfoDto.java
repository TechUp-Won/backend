package com.example.WonkaoTalk.domain.order.dto;

import org.springframework.data.domain.Page;

public record PageInfoDto(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

  public static PageInfoDto from(Page<?> page) {
    return new PageInfoDto(
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext()
    );
  }
}

