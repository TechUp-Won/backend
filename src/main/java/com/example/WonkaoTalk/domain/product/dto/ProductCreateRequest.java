package com.example.WonkaoTalk.domain.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductCreateRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull Long categoryId,
    String thumbnailKey,
    @NotNull @Min(0) Integer price,
    @Min(0) @Max(100) Integer discountRate,
    String detail,
    Integer stock,
    @Valid List<ImageRequest> images,
    @Valid List<OptionGroupRequest> optionGroups,
    @Valid List<VariantRequest> variants
) {

  public record ImageRequest(
      @NotBlank String objectKey,
      @NotNull Integer sortOrder
  ) {}

  public record OptionGroupRequest(
      @NotBlank String name,
      @NotNull Integer sortOrder,
      @NotEmpty @Valid List<OptionRequest> options
  ) {}

  public record OptionRequest(
      @NotBlank String name
  ) {}

  public record VariantRequest(
      @NotNull @NotEmpty List<String> optionNames,
      @NotNull @Min(1) Integer stock
  ) {}
}
