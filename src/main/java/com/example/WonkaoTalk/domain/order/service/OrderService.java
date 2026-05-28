package com.example.WonkaoTalk.domain.order.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateRequest.DeliveryRequestDto;
import com.example.WonkaoTalk.domain.order.dto.OrderCreateResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderDetailResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderDetailResponse.PaymentInfoDto;
import com.example.WonkaoTalk.domain.order.dto.OrderInfoDto;
import com.example.WonkaoTalk.domain.order.dto.OrderItemDto;
import com.example.WonkaoTalk.domain.order.dto.OrderItemInfoDto;
import com.example.WonkaoTalk.domain.order.dto.OrderListResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderListResponse.OrderSummaryDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewRequest;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponse;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponse.OrderPreviewItemDto;
import com.example.WonkaoTalk.domain.order.dto.OrderPreviewResponse.SummaryDto;
import com.example.WonkaoTalk.domain.order.dto.PageInfoDto;
import com.example.WonkaoTalk.domain.order.entity.Delivery;
import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.order.entity.OrderItem;
import com.example.WonkaoTalk.domain.order.repo.DeliveryRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.order.repo.OrderRepo;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import com.example.WonkaoTalk.domain.payment.service.PaymentService;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductVariantRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final UserRepo userRepo;

  private final ProductVariantRepo productVariantRepo;
  private final OrderRepo orderRepo;
  private final OrderItemRepo orderItemRepo;
  private final DeliveryRepo deliveryRepo;
  private final PaymentRepo paymentRepo;

  private final PaymentService paymentService;

  // 주문 생성 로직 작성
  // 응답값으로 Order로 생성 요청한 값들의 성공적으로 생성 되었는지만 전달해주면됨.
  // 주문 생성 시 재고 차감 진행. 만약 주문이 실패로 끝나면 재고 원상복귀.
  @Transactional(rollbackFor = BusinessException.class)
  public OrderCreateResponse createOrder(Long userId,
      OrderCreateRequest requestDto) {
    // 유저 정보 검색
    User user = userRepo.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

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
    // TODO: 재고 감소로직 있어야함. (임시 컬럼만들어서 진행하던지 뭘 하던지 할듯..)

    // 7. 주문 금액 계산 -> 금액 계산위해 item 생성
    List<OrderPreviewItemDto> orderItems = createOrderPreviewItems(requestDto.items(),
        productVariants);

    SummaryDto summaryDto = createSummary(orderItems);

    // 8. 포인트 사용 금액 검증
    // TODO: 포인트 도메인 구현 전까지 pointUsedAmount는 0만 허용하거나 0으로 처리
    Long pointUsedAmount = 0L;
//    Long pointUsedAmount = requestDto.pointUsedAmount() == 0 ? 0L : requestDto.pointUsedAmount();
    // TODO : 포인트 값 차감시키기.

    // 9. Order 생성
    String orderNumber = generateUniqueOrderNumber();
    String orderTitle = generateOrderTitle(orderItems);

    Order order = Order.createOrder(
        orderNumber,
        user,
        summaryDto.originalAmount(),
        summaryDto.discountAmount(),
        pointUsedAmount,
        summaryDto.finalAmount(),
        orderTitle
    );

    // 주문 생성완료 + 결제 대기상태
    order.markPaymentPending();

    // 이렇게 객체 새로 생성해서 부여하는 방식이 옳은 방식일지 고민해볼 필요 있을듯
    Order savedOrder = orderRepo.save(order);
    orderItemRepo.saveAll(createOrderItems(savedOrder, orderItems, productVariants));

    // 12. Payment 생성
    // status = READY
    // tossOrderId 생성
    // idempotencyKey 생성
    // totalAmount = order.finalAmount
    Payment payment = paymentService.createReadyPayment(savedOrder);

    saveDelivery(savedOrder, requestDto.delivery());

    // 13. 주문 생성 응답 반환
    // orderId, orderNumber, paymentId, tossOrderId, amount, orderName
    return new OrderCreateResponse(
        new OrderInfoDto(
            savedOrder.getOrderId(),
            savedOrder.getOrderNumber(),
            savedOrder.getOrderTitle(),
            savedOrder.getOriginalAmount(),
            savedOrder.getDiscountAmount(),
            savedOrder.getPointUsedAmount(),
            savedOrder.getFinalAmount()
        ),
        new OrderCreateResponse.PaymentCreateInfoDto(
            payment.getPaymentId(),
            payment.getTossOrderId(),
            payment.getTotalAmount(),
            savedOrder.getOrderTitle()
        )
    );
  }

  // 이때는 결제가 이루어지지 않기때문에 재고 조회에 대한 lock을 크게 고려하지 않아도 될듯.
  @Transactional(readOnly = true)
  public OrderPreviewResponse previewOrder(OrderPreviewRequest requestDto) {
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
    return new OrderPreviewResponse(orderPreviewItemDtos, summaryDto);
  }

  // 주문 목록 가지고 오는 메서드
  @Transactional(readOnly = true)
  public OrderListResponse getOrders(Long userId, Pageable pageable) {
    Page<Order> orderPage = orderRepo.findByUserId(userId, pageable);

    // 조회 한 오더 정보를 pageInfo와 Summary로 만들어서 OrderListResponse로 만들어야함
    // 먼저 summary 를 만들어야함
    List<OrderSummaryDto> summaryDtos = orderPage.getContent().stream()
        .map(order -> new OrderSummaryDto(order.getOrderId(), order.getOrderNumber(),
            order.getOrderTitle(), order.getOrderStatus(), order.getFinalAmount(),
            order.getCreatedAt()))
        .toList();
    // 그다음은 PageInfo인데 이건 어떻게만들지? from 만들어 놨잖아
    return new OrderListResponse(
        summaryDtos,
        PageInfoDto.from(orderPage)
    );
  }

  // 주문 상세정보 전달 메서드
  @Transactional(readOnly = true)
  public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
    Order order = orderRepo.findByUserIdAndOrderId(userId, orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

    List<OrderItem> orderItems = orderItemRepo.findByOrder(order);

    List<Payment> payments = paymentRepo.findByOrder_OrderIdOrderByCreatedAtDesc(orderId);

    if (orderItems.isEmpty()) {
      throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
    }

    return new OrderDetailResponse(
        OrderInfoDto.from(order),
        orderItems.stream().map(OrderItemInfoDto::from).toList(),
        payments.stream().map(PaymentInfoDto::from).toList()
    );

  }

  // 주문 정보 삭제 메서드
  @Transactional
  public void deleteOrder(Long userId, Long orderId) {
    Order order = orderRepo.findByUserIdAndOrderId(userId, orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

    orderRepo.delete(order);
  }

  // 옵션 중복 검증 메서드
  public void validateDuplicateVariant(List<OrderItemDto> items) {
    Set<Long> validateIds = new HashSet<>();

    for (OrderItemDto item : items) {
      if (!validateIds.add(item.variantId())) {
        // TODO : Exception 따로 만들어야함.
        throw new BusinessException(ErrorCode.BAD_REQUEST);
      }
    }
  }

  public List<Long> extractVariantIds(List<OrderItemDto> items) {
    return items.stream()
        .map(OrderItemDto::variantId)
        .toList();
  }

  // variantId 목록으로 ProductVariant 조회 후 Map으로 변환
  public Map<Long, ProductVariant> findVariantMapByIds(List<Long> variantIds) {

    List<ProductVariant> variants = productVariantRepo.findAllById(variantIds);

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
  public void validateVariantStock(List<OrderItemDto> items,
      Map<Long, ProductVariant> variantMap) {
    // 먼저 옵션 Stock들을 읽어와야함. Map으로 가지고 오면 이것도 비교하기 좋을것같음.
    // 요청 받은 dto에서 id->quantity 를 가지고 와서 검증해야함.
    for (OrderItemDto item : items) {
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
      List<OrderItemDto> items,
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

  private List<OrderItem> createOrderItems(
      Order order,
      List<OrderPreviewItemDto> items,
      Map<Long, ProductVariant> variantMap
  ) {
    return items.stream()
        .map(item -> OrderItem.createOrderItem(
            order,
            variantMap.get(item.variantId()),
            item.productName(),
            item.variantName(),
            item.discountedPrice(),
            item.quantity(),
            item.thumbnailUrl()
        ))
        .toList();
  }

  private void saveDelivery(Order order, DeliveryRequestDto deliveryRequestDto) {
    Delivery delivery = Delivery.createDelivery(
        order,
        deliveryRequestDto.recipientName(),
        deliveryRequestDto.recipientPhone(),
        deliveryRequestDto.zipcode(),
        deliveryRequestDto.address(),
        deliveryRequestDto.addressDetail(),
        deliveryRequestDto.memo()
    );

    deliveryRepo.save(delivery);
  }

  // 주문 번호 생성
  private String generateOrderNumber() {
    String timestamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

    String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    return "ORD-" + timestamp + "-" + uuid;
  }

  // 주문번호 unique 검증
  private String generateUniqueOrderNumber() {
    for (int i = 0; i < 5; i++) {
      String orderNumber = generateOrderNumber();

      if (!orderRepo.existsByOrderNumber(orderNumber)) {
        return orderNumber;
      }
    }
    throw new BusinessException(ErrorCode.SERVER_ERROR);
  }

  // 주문 제목 생성
  private String generateOrderTitle(List<OrderPreviewItemDto> items) {
    if (items.size() == 1) {
      return items.getFirst().productName();
    }

    return items.getFirst().productName() + " 외 " + (items.size() - 1) + "건";
  }

}
