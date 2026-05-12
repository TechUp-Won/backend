package com.example.WonkaoTalk.domain.order.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderItemRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponseDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponseDto.OrderPreviewItemDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponseDto.SummaryDto;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final ProductVariantRepository productVariantRepository;

  // 주문 생성 로직 작성
  // 응답값으로 Order로 생성 요청한 값들의 성공적으로 생성 되었는지만 전달해주면됨.
  // 주문 생성 시 재고 차감 진행. 만약 주문이 실패로 끝나면 재고 원상복귀.
  public void createOrder(Long userId,
      OrderCreateRequestDto dto) {
    // 주문 번호 생
  }

  // 이때는 결제가 이루어지지 않기때문에 재고 조회에 대한 lock을 크게 고려하지 않아도 될듯.
  @Transactional(readOnly = true)
  public OrderPreviewResponseDto previewOrder(OrderPreviewRequestDto requestDto) {
    // 1. variantId 중복 검증
    validateDuplicateVariant(requestDto.items());

    // 2. ProductVariant 조회
    List<Long> requestVariantIds = extractVariantIds(requestDto.items());
    Map<Long, ProductVariant> productVariants = findVariantMapByIds(requestVariantIds);

    // 3. 조회하지 않는 variantId 검증 에러처리
    validateVariantExist(requestVariantIds, productVariants);

    // 4. 판매자 상태 확인 -> 지금은 그냥 하드코딩 더미데이터로 해결하기
    validateSellerStatus();

    // 추가. 상품 상태 확인
    validateVariantSaleStatus(productVariants);

    // 5. 재고 상태 확인 (요청 수량에 맞게 주문할 수 있는지)
    validateVariantStock(requestDto.items(), productVariants);

    // 6. item 응답 생성
    List<OrderPreviewItemDto> orderPreviewItemDtos = createOrderPreviewItems(requestDto.items(),
        productVariants);

    // 7. summary 계산
    SummaryDto summaryDto = createSummary(orderPreviewItemDtos);

    // 8. response dto 생성 및 return
    return new OrderPreviewResponseDto(orderPreviewItemDtos, summaryDto);
  }

  // 옵션 중복 검증 메서드
  public void validateDuplicateVariant(List<OrderItemRequestDto> items) {
    Set<Long> validateIds = new HashSet<>();

    for (OrderItemRequestDto item : items) {
      if (!validateIds.add(item.variantId())) {
        // TODO : Exception 따로 만들어야함.
        throw new BusinessException(ErrorCode.BAD_REQUEST);
      }
    }
  }

  public List<Long> extractVariantIds(List<OrderItemRequestDto> items) {
    return items.stream()
        .map(OrderItemRequestDto::variantId)
        .toList();
  }

  // variantId 목록으로 ProductVariant 조회 후 Map으로 변환
  public Map<Long, ProductVariant> findVariantMapByIds(List<Long> variantIds) {

    List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);

    return variants.stream().collect(Collectors.toMap(
        ProductVariant::getId,
        variant -> variant
    ));
  }

  /**
   * 요청 옵션 Id 유효성 검증. findAllById로 조회 하면 존재하지 않는 id 조회 시 exception이 나오지 않음.
   *
   * @param variantIds 요청 단계에서 온 ids
   * @param variantMap 조회 후 return 받은 VariantMap
   */
  public void validateVariantExist(List<Long> variantIds, Map<Long, ProductVariant> variantMap) {
    for (Long variantId : variantIds) {
      if (!variantMap.containsKey(variantId)) {
        throw new BusinessException(ErrorCode.NOT_FOUND);
      }
    }
  }

  // 판매자 상태 확인
  // TODO: 추후 구현
  public void validateSellerStatus() {
  }

  // 판매 상태 확인
  public void validateVariantSaleStatus(Map<Long, ProductVariant> variantMap) {
    for (ProductVariant variant : variantMap.values()) {
      if (variant.getStatus() != SaleStatus.ON_SALE) {
        throw new BusinessException(ErrorCode.PROD_VARIANT_UNAVAILABLE);
      }
    }
  }

  // 재고 상태 확인
  public void validateVariantStock(List<OrderItemRequestDto> items,
      Map<Long, ProductVariant> variantMap) {
    // 먼저 옵션 Stock들을 읽어와야함. Map으로 가지고 오면 이것도 비교하기 좋을것같음.
    // 요청 받은 dto에서 id->quantity 를 가지고 와서 검증해야함.
    for (OrderItemRequestDto item : items) {
      Long variantId = item.variantId();
      Integer requestedQuantity = item.quantity();

      ProductVariant productVariant = variantMap.get(variantId);

      if (productVariant.getStock() < requestedQuantity) {
        throw new BusinessException(ErrorCode.PROD_STOCK_INSUFFICIENT);
      }
    }
  }

  // orderPreviewItems List 생성
  public List<OrderPreviewItemDto> createOrderPreviewItems(
      List<OrderItemRequestDto> items,
      Map<Long, ProductVariant> variantMap
  ) {
    // TODO: Lazy Loading으로 성능개선이 필요할수도..??
    return items.stream()
        .map(item -> {
          ProductVariant productVariant = variantMap.get(item.variantId());
          Product product = productVariant.getProduct();

          // 계산용 변수들 선언
          int quantity = item.quantity();
          long price = product.getPrice().longValue();
          long discountPrice = product.getDiscountedPrice().longValue();

          long itemOriginalAmount = price * quantity;
          long itemFinalAmount = discountPrice * quantity;
          long itemDiscountAmount = itemOriginalAmount - itemFinalAmount;

          return new OrderPreviewItemDto(
              product.getId(),
              productVariant.getId(),
              product.getName(),
              productVariant.getName(),
              product.getThumbnail(),
              price,
              discountPrice,
              quantity,
              itemOriginalAmount,
              itemDiscountAmount,
              itemFinalAmount
          );
        })
        .toList();
  }

  public SummaryDto createSummary(List<OrderPreviewItemDto> items) {
    long originalAmount = items.stream()
        .mapToLong(OrderPreviewItemDto::itemOriginalAmount)
        .sum();
    long discountAmount = items.stream()
        .mapToLong(OrderPreviewItemDto::itemDiscountAmount)
        .sum();
    long finalAmount = originalAmount - discountAmount;

    return new SummaryDto(originalAmount, discountAmount, finalAmount);
  }

}
