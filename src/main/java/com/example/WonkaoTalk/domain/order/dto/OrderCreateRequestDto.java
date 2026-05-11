package com.example.WonkaoTalk.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreateRequestDto {

  @Valid
  @NotEmpty
  private List<OrderItemRequest> items;

  @Valid
  @NotNull
  private DeliveryRequest delivery;

  // 일단은 static으로 선언해서 사용. -> 아직은 해당 DTO에서만 사용하기때문..(요청의 일부)
  @Getter
  @NoArgsConstructor
  public static class OrderItemRequest {

    @NotNull
    private Long variantId;

    @NotNull
    @Min(1) //수량의 최소값은 1로 설정
    private Integer quantity;
  }

  @Getter
  @NoArgsConstructor
  public static class DeliveryRequest {

    @NotBlank
    private String recipientName;

    @NotBlank
    private String recipientPhone;

    @NotBlank
    private String zipcode;

    @NotBlank
    private String address;

    @NotBlank
    private String addressDetail;

    // 메모는 필수사항이 아님
    private String memo;
  }


}
