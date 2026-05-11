// 주문 전 주문 요청서 생성용 API
package com.example.WonkaoTalk.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderPreviewRequestDto {

  @Valid
  @NotEmpty
  private List<OrderItemRequest> items;


  // TODO : OrderCreateRequest에서도 계속 사용한다면 이 함수는 따로 빼서 관리하도록...
  @Getter
  @NoArgsConstructor
  public static class OrderItemRequest {

    @NotNull
    private Long variantId;

    @NotNull
    @Min(1) //수량의 최소값은 1로 설정
    private Integer quantity;
  }
}
