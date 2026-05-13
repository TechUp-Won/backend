package com.example.WonkaoTalk.domain.shipping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressCreateRequest;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressListResponse;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressResponse;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressUpdateRequest;
import com.example.WonkaoTalk.domain.shipping.entity.ShippingAddress;
import com.example.WonkaoTalk.domain.shipping.repo.ShippingAddressRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShippingAddressServiceTest {

  @InjectMocks
  private ShippingAddressService shippingAddressService;

  @Mock
  private ShippingAddressRepo shippingAddressRepo;

  @Mock
  private UserRepo userRepo;

  private User user;
  private ShippingAddress address;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .nickname("테스터").name("홍길동").phone("010-1111-1111")
        .build();
    ReflectionTestUtils.setField(user, "id", 1L);

    address = ShippingAddress.builder()
        .user(user)
        .recipientName("홍길동")
        .recipientPhone("010-1234-5678")
        .zipCode("06234")
        .address1("서울특별시 강남구 테헤란로 427")
        .build();
    ReflectionTestUtils.setField(address, "id", 10L);
    ReflectionTestUtils.setField(address, "isDefault", false);
  }

  // ============================
  // getList
  // ============================

  @Test
  @DisplayName("배송지 목록 조회 성공")
  void getList_Success() {
    // given
    ShippingAddress address2 = ShippingAddress.builder()
        .user(user).recipientName("홍길동").recipientPhone("010-9999-8888")
        .zipCode("04524").address1("서울특별시 중구 세종대로 110").isDefault(true)
        .build();
    ReflectionTestUtils.setField(address2, "id", 20L);

    given(shippingAddressRepo.findByUserId(1L)).willReturn(List.of(address, address2));

    // when
    ShippingAddressListResponse response = shippingAddressService.getList(1L);

    // then
    assertThat(response.addresses()).hasSize(2);
    assertThat(response.addresses().get(0).shippingAddressId()).isEqualTo(10L);
    assertThat(response.addresses().get(1).shippingAddressId()).isEqualTo(20L);
  }

  @Test
  @DisplayName("배송지가 없으면 빈 목록을 반환한다")
  void getList_Empty_ReturnsEmptyList() {
    // given
    given(shippingAddressRepo.findByUserId(1L)).willReturn(List.of());

    // when
    ShippingAddressListResponse response = shippingAddressService.getList(1L);

    // then
    assertThat(response.addresses()).isEmpty();
  }

  // ============================
  // create
  // ============================

  @Test
  @DisplayName("첫 번째 배송지 등록 시 기본 배송지로 자동 설정된다")
  void create_FirstAddress_SetsDefault() {
    // given
    ShippingAddressCreateRequest request = new ShippingAddressCreateRequest(
        "홍길동", "010-1234-5678", "06234", "서울특별시 강남구 테헤란로 427", null, null);

    ShippingAddress saved = ShippingAddress.builder()
        .user(user).recipientName("홍길동").recipientPhone("010-1234-5678")
        .zipCode("06234").address1("서울특별시 강남구 테헤란로 427").isDefault(true)
        .build();
    ReflectionTestUtils.setField(saved, "id", 10L);

    given(userRepo.findByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(shippingAddressRepo.countByUserId(1L)).willReturn(0L);
    given(shippingAddressRepo.save(any(ShippingAddress.class))).willReturn(saved);

    // when
    ShippingAddressResponse response = shippingAddressService.create(1L, request);

    // then
    assertThat(response.isDefault()).isTrue();
    verify(shippingAddressRepo).save(any(ShippingAddress.class));
  }

  @Test
  @DisplayName("기존 배송지가 있을 때 등록하면 기본 배송지가 아니다")
  void create_SecondAddress_NotDefault() {
    // given
    ShippingAddressCreateRequest request = new ShippingAddressCreateRequest(
        "홍길동", "010-9999-8888", "06234", "서울특별시 강남구 선릉로 100", null, null);

    ShippingAddress saved = ShippingAddress.builder()
        .user(user).recipientName("홍길동").recipientPhone("010-9999-8888")
        .zipCode("06234").address1("서울특별시 강남구 선릉로 100").isDefault(false)
        .build();
    ReflectionTestUtils.setField(saved, "id", 11L);

    given(userRepo.findByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(shippingAddressRepo.countByUserId(1L)).willReturn(1L);
    given(shippingAddressRepo.save(any(ShippingAddress.class))).willReturn(saved);

    // when
    ShippingAddressResponse response = shippingAddressService.create(1L, request);

    // then
    assertThat(response.isDefault()).isFalse();
  }

  @Test
  @DisplayName("존재하지 않는 유저로 배송지 등록 시 예외가 발생한다")
  void create_UserNotFound_ThrowsException() {
    // given
    ShippingAddressCreateRequest request = new ShippingAddressCreateRequest(
        "홍길동", "010-1234-5678", "06234", "서울특별시 강남구 테헤란로 427", null, null);

    given(userRepo.findByIdForUpdate(1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> shippingAddressService.create(1L, request))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
  }

  // ============================
  // update
  // ============================

  @Test
  @DisplayName("배송지 수정 성공")
  void update_Success() {
    // given
    ShippingAddressUpdateRequest request = new ShippingAddressUpdateRequest(
        "김철수", "010-9999-8888", null, null, null, "부재 시 문 앞에 놔주세요", null);

    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(address));

    // when
    ShippingAddressResponse response = shippingAddressService.update(1L, 10L, request);

    // then
    assertThat(address.getRecipientName()).isEqualTo("김철수");
    assertThat(address.getRecipientPhone()).isEqualTo("010-9999-8888");
    assertThat(address.getMemo()).isEqualTo("부재 시 문 앞에 놔주세요");
    assertThat(response).isNotNull();
  }

  @Test
  @DisplayName("isDefault: true 전달 시 기존 기본 배송지가 해제되고 해당 배송지가 기본으로 설정된다")
  void update_WithIsDefaultTrue_ChangesDefault() {
    // given
    ShippingAddressUpdateRequest request = new ShippingAddressUpdateRequest(
        null, null, null, null, null, null, true);

    ShippingAddress previousDefault = ShippingAddress.builder()
        .user(user).recipientName("이전기본").recipientPhone("010-0000-0000")
        .zipCode("12345").address1("기존 주소").isDefault(true)
        .build();
    ReflectionTestUtils.setField(previousDefault, "id", 20L);

    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(address));
    given(shippingAddressRepo.findDefaultByUserId(1L)).willReturn(Optional.of(previousDefault));

    // when
    shippingAddressService.update(1L, 10L, request);

    // then
    assertThat(previousDefault.isDefault()).isFalse();
    assertThat(address.isDefault()).isTrue();
  }

  @Test
  @DisplayName("존재하지 않는 배송지 수정 시 예외가 발생한다")
  void update_NotFound_ThrowsException() {
    // given
    ShippingAddressUpdateRequest request = new ShippingAddressUpdateRequest(
        "홍길동", null, null, null, null, null, null);

    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> shippingAddressService.update(1L, 10L, request))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.SHIP_NOT_FOUND.getMessage());
  }

  // ============================
  // delete
  // ============================

  @Test
  @DisplayName("기본 배송지가 아닌 배송지 삭제 성공")
  void delete_NotDefault_Success() {
    // given
    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(address));

    // when
    shippingAddressService.delete(1L, 10L);

    // then
    verify(shippingAddressRepo).delete(address);
  }

  @Test
  @DisplayName("배송지가 1개뿐인 기본 배송지는 삭제할 수 있다")
  void delete_OnlyDefaultAddress_Success() {
    // given
    ReflectionTestUtils.setField(address, "isDefault", true);

    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(address));
    given(shippingAddressRepo.countByUserId(1L)).willReturn(1L);

    // when
    shippingAddressService.delete(1L, 10L);

    // then
    verify(shippingAddressRepo).delete(address);
  }

  @Test
  @DisplayName("배송지가 여러 개일 때 기본 배송지 삭제 시 예외가 발생한다")
  void delete_DefaultWithMultipleAddresses_ThrowsException() {
    // given
    ReflectionTestUtils.setField(address, "isDefault", true);

    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(address));
    given(shippingAddressRepo.countByUserId(1L)).willReturn(2L);

    // when & then
    assertThatThrownBy(() -> shippingAddressService.delete(1L, 10L))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.SHIP_CANNOT_DELETE_DEFAULT.getMessage());
  }

  @Test
  @DisplayName("존재하지 않는 배송지 삭제 시 예외가 발생한다")
  void delete_NotFound_ThrowsException() {
    // given
    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> shippingAddressService.delete(1L, 10L))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.SHIP_NOT_FOUND.getMessage());
  }

  // ============================
  // setDefault
  // ============================

  @Test
  @DisplayName("기본 배송지 설정 성공 - 기존 기본 배송지가 해제된다")
  void setDefault_Success() {
    // given
    ShippingAddress previousDefault = ShippingAddress.builder()
        .user(user).recipientName("이전기본").recipientPhone("010-0000-0000")
        .zipCode("12345").address1("기존 주소").isDefault(true)
        .build();
    ReflectionTestUtils.setField(previousDefault, "id", 20L);

    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(address));
    given(shippingAddressRepo.findDefaultByUserId(1L)).willReturn(Optional.of(previousDefault));

    // when
    ShippingAddressResponse response = shippingAddressService.setDefault(1L, 10L);

    // then
    assertThat(previousDefault.isDefault()).isFalse();
    assertThat(address.isDefault()).isTrue();
    assertThat(response).isNotNull();
  }

  @Test
  @DisplayName("존재하지 않는 배송지를 기본으로 설정 시 예외가 발생한다")
  void setDefault_NotFound_ThrowsException() {
    // given
    given(shippingAddressRepo.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> shippingAddressService.setDefault(1L, 10L))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.SHIP_NOT_FOUND.getMessage());
  }
}
