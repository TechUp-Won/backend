package com.example.WonkaoTalk.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequestDto(
    @Valid
    @NotEmpty
    List<OrderItemRequestDto> items,

    @Valid
    @NotNull
    DeliveryRequest delivery
) {

  // 일단은 static으로 선언해서 사용. -> 아직은 해당 DTO에서만 사용하기때문..(요청의 일부)
  public record DeliveryRequest(
      @NotBlank
      String recipientName,

      @NotBlank
      String recipientPhone,

      @NotBlank
      String zipcode,

      @NotBlank
      String address,

      @NotBlank
      String addressDetail,

      // 메모는 필수사항이 아님
      String memo
  ) {
  }
}
