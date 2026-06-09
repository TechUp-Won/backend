package com.example.WonkaoTalk.domain.shipping.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.WonkaoTalk.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShippingAddressTest {

  // ── update() ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("null이 아닌 필드는 전달한 값으로 변경된다")
  void update_nonNullFields_areUpdated() {
    // given
    ShippingAddress address = buildAddress("홍길동", "010-1111-2222", "06234",
        "서울시 강남구", "101동", false, "문 앞");

    // when
    address.update("김철수", "010-9999-8888", "12345", "부산시 해운대구", "202호", "경비실 맡김");

    // then
    assertThat(address.getRecipientName()).isEqualTo("김철수");
    assertThat(address.getRecipientPhone()).isEqualTo("010-9999-8888");
    assertThat(address.getZipCode()).isEqualTo("12345");
    assertThat(address.getAddress1()).isEqualTo("부산시 해운대구");
    assertThat(address.getAddress2()).isEqualTo("202호");
    assertThat(address.getMemo()).isEqualTo("경비실 맡김");
  }

  @Test
  @DisplayName("recipientName, recipientPhone, zipCode, address1이 null이면 기존 값을 유지한다")
  void update_nullRequiredFields_preservesExistingValues() {
    // given
    ShippingAddress address = buildAddress("홍길동", "010-1111-2222", "06234",
        "서울시 강남구", "101동", false, "문 앞");

    // when
    address.update(null, null, null, null, "새주소2", "새메모");

    // then
    assertThat(address.getRecipientName()).isEqualTo("홍길동");
    assertThat(address.getRecipientPhone()).isEqualTo("010-1111-2222");
    assertThat(address.getZipCode()).isEqualTo("06234");
    assertThat(address.getAddress1()).isEqualTo("서울시 강남구");
  }

  @Test
  @DisplayName("address2와 memo는 null을 전달해도 null로 덮어쓴다")
  void update_nullAddress2AndMemo_overwritesWithNull() {
    // given
    ShippingAddress address = buildAddress("홍길동", "010-1111-2222", "06234",
        "서울시 강남구", "101동", false, "문 앞");

    // when: address2와 memo에 null 전달
    address.update(null, null, null, null, null, null);

    // then: null로 덮어씌워진다
    assertThat(address.getAddress2()).isNull();
    assertThat(address.getMemo()).isNull();
  }

  // ── setAsDefault() / unsetDefault() ──────────────────────────────────────────

  @Test
  @DisplayName("setAsDefault 호출 시 isDefault가 true로 변경된다")
  void setAsDefault_setsIsDefaultTrue() {
    // given
    ShippingAddress address = buildAddress("홍길동", "010-1111-2222", "06234",
        "서울시 강남구", "101동", false, null);

    // when
    address.setAsDefault();

    // then
    assertThat(address.isDefault()).isTrue();
  }

  @Test
  @DisplayName("unsetDefault 호출 시 isDefault가 false로 변경된다")
  void unsetDefault_setsIsDefaultFalse() {
    // given
    ShippingAddress address = buildAddress("홍길동", "010-1111-2222", "06234",
        "서울시 강남구", "101동", true, null);

    // when
    address.unsetDefault();

    // then
    assertThat(address.isDefault()).isFalse();
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private ShippingAddress buildAddress(String name, String phone, String zipCode,
      String address1, String address2, boolean isDefault, String memo) {
    return ShippingAddress.builder()
        .user(mock(User.class))
        .recipientName(name)
        .recipientPhone(phone)
        .zipCode(zipCode)
        .address1(address1)
        .address2(address2)
        .isDefault(isDefault)
        .memo(memo)
        .build();
  }
}
