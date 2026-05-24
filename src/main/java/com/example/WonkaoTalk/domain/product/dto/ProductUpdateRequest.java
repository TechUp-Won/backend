package com.example.WonkaoTalk.domain.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductUpdateRequest(
    @Size(max = 255) String name,
    Long categoryId,
    String thumbnailKey,
    @Min(0) Integer price,
    @Min(0) @Max(100) Integer discountRate,
    String detail,
    String status,
    @Valid List<ImageRequest> images
) {

  public record ImageRequest(
      Long imageId,
      String objectKey,
      @NotNull Integer sortOrder
  ) {}
}
