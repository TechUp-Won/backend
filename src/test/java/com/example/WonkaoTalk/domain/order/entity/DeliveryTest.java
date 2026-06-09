package com.example.WonkaoTalk.domain.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryTest {

  @Test
  @DisplayName("배송 정보 생성 시 PREPARING 상태로 초기화된다.")
  void createDelivery_InitializesPreparingStatus() {
    // given
    Order order = Order.createOrder("ORD-1234", null, 10000L, 0L, 0L, 10000L, "테스트 상품");

    // when
    Delivery delivery = Delivery.createDelivery(order, "홍길동", "010-1234-5678", "06234",
        "서울특별시 강남구 테헤란로", "101동 1001호", "문 앞에 놔주세요");

    // then
    assertThat(delivery.getOrder()).isEqualTo(order);
    assertThat(delivery.getRecipientName()).isEqualTo("홍길동");
    assertThat(delivery.getRecipientPhone()).isEqualTo("010-1234-5678");
    assertThat(delivery.getZipcode()).isEqualTo("06234");
    assertThat(delivery.getAddress()).isEqualTo("서울특별시 강남구 테헤란로");
    assertThat(delivery.getAddressDetail()).isEqualTo("101동 1001호");
    assertThat(delivery.getMemo()).isEqualTo("문 앞에 놔주세요");
    assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
  }

  @Test
  @DisplayName("배송 시작 처리 시 택배사와 운송장 번호를 저장하고 SHIPPED 상태로 변경한다.")
  void markShipped_UpdatesCarrierInfoAndStatus() {
    // given
    Delivery delivery = Delivery.createDelivery(null, "홍길동", "010-1234-5678", "06234",
        "서울특별시 강남구 테헤란로", "101동 1001호", null);

    // when
    delivery.markShipped("CJ대한통운", "1234567890");

    // then
    assertThat(delivery.getDeliveryCompany()).isEqualTo("CJ대한통운");
    assertThat(delivery.getTrackingNumber()).isEqualTo("1234567890");
    assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);
  }

  @Test
  @DisplayName("배송 중과 배송 완료 상태로 변경할 수 있다.")
  void markInTransitAndDelivered_UpdatesDeliveryStatus() {
    // given
    Delivery delivery = Delivery.createDelivery(null, "홍길동", "010-1234-5678", "06234",
        "서울특별시 강남구 테헤란로", "101동 1001호", null);

    // when
    delivery.markInTransit();

    // then
    assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);

    // when
    delivery.markDelivered();

    // then
    assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
  }
}
