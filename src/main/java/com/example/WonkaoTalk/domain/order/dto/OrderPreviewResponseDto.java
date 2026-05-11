package com.example.WonkaoTalk.domain.order.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPreviewResponseDto {

  private List<OrderPreviewItemDto> items;
  private SummaryDto summary;


  @Getter
  @Builder
  public static class OrderPreviewItemDto {

    private Long productId;
    private Long variantId;

    // 상품 명
    private String productName;

    // 옵션 명 (선택한 옵션을 문자열로 합쳐서 만들어서 전송해준다고 가정함.)
    // 이건 그냥 List 로받아서 처리하는게 나을지 고민 필요함.
    private String variantName;

    // 상품 썸네일 이미지 url
    private String thumbnailUrl;

    //단가정보
    // 가격
    private Integer price;

    // 할인가격
    private Integer discountedPrice;

    // 수량
    private Integer quantity;


    //수량 계산한 가격
    // 원가 합계
    private Integer itemOriginalAmount;
    // 할인 합계
    private Integer itemDiscountAmount;
    // 최종 금액
    private Integer itemFinalAmount;
  }

  @Getter
  @Builder
  public static class SummaryDto {

    private Integer originalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
  }


}
