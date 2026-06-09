package com.example.WonkaoTalk.domain.shipping.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressCreateRequest;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressUpdateRequest;
import com.example.WonkaoTalk.domain.shipping.entity.ShippingAddress;
import com.example.WonkaoTalk.domain.shipping.repo.ShippingAddressRepo;
import com.example.WonkaoTalk.domain.shipping.service.ShippingAddressService;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainerConfig.class)
@Transactional
class ShippingAddressIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ShippingAddressService shippingAddressService;

  @Autowired
  private ShippingAddressRepo shippingAddressRepo;

  private static int userSeq = 0;

  private User user;
  private Long userId;

  @BeforeEach
  void setUp() {
    user = saveUser("테스트유저");
    userId = user.getId();
    em.flush();
    em.clear();
  }

  // ── 첫 배송지 등록 시 자동 기본 설정 ───────────────────────────────────────────

  @Test
  @DisplayName("첫 배송지 등록 시 isDefault가 true로 DB에 저장된다")
  void create_firstAddress_isPersisted_asDefault() {
    // when
    shippingAddressService.create(userId, createRequest("홍길동", "010-1234-5678", "06234",
        "서울시 강남구 테헤란로", "101동", null));
    em.flush();
    em.clear();

    // then: 실제 DB에서 isDefault 값 확인
    List<ShippingAddress> addresses = shippingAddressRepo.findByUserId(userId);
    assertThat(addresses).hasSize(1);
    assertThat(addresses.get(0).isDefault()).isTrue();
  }

  @Test
  @DisplayName("두 번째 배송지 등록 시 isDefault가 false로 DB에 저장된다")
  void create_secondAddress_isPersisted_asNotDefault() {
    // given: 첫 번째 배송지 등록
    shippingAddressService.create(userId, createRequest("홍길동", "010-1234-5678", "06234",
        "서울시 강남구 테헤란로", "101동", null));

    // when: 두 번째 배송지 등록
    shippingAddressService.create(userId, createRequest("김철수", "010-9999-8888", "12345",
        "부산시 해운대구", null, null));
    em.flush();
    em.clear();

    // then: 두 번째 배송지는 기본이 아님
    List<ShippingAddress> addresses = shippingAddressRepo.findByUserId(userId);
    assertThat(addresses).hasSize(2);
    long defaultCount = addresses.stream().filter(ShippingAddress::isDefault).count();
    assertThat(defaultCount).isEqualTo(1);
    assertThat(addresses.stream()
        .filter(a -> a.getRecipientName().equals("김철수"))
        .findFirst().orElseThrow()
        .isDefault()).isFalse();
  }

  // ── 기본 배송지 전환 시 기존 기본 해제 (영속성 반영 검증) ──────────────────────

  @Test
  @DisplayName("setDefault 호출 시 기존 기본 배송지가 DB에서 해제되고 새 배송지가 기본으로 설정된다")
  void setDefault_unsetsPreviousDefault_andSetsNewDefault_inDB() {
    // given: 배송지 2개 등록 (첫 번째가 기본)
    shippingAddressService.create(userId, createRequest("홍길동", "010-1234-5678", "06234",
        "서울시 강남구", null, null));
    shippingAddressService.create(userId, createRequest("김철수", "010-9999-8888", "12345",
        "부산시 해운대구", null, null));
    em.flush();
    em.clear();

    List<ShippingAddress> before = shippingAddressRepo.findByUserId(userId);
    ShippingAddress defaultAddr = before.stream().filter(ShippingAddress::isDefault).findFirst().orElseThrow();
    ShippingAddress otherAddr = before.stream().filter(a -> !a.isDefault()).findFirst().orElseThrow();

    // when: 기본이 아닌 배송지를 기본으로 설정
    shippingAddressService.setDefault(userId, otherAddr.getId());
    em.flush();
    em.clear();

    // then: DB에서 기존 기본이 해제, 새 배송지가 기본
    List<ShippingAddress> after = shippingAddressRepo.findByUserId(userId);
    ShippingAddress newDefault = after.stream()
        .filter(a -> a.getId().equals(otherAddr.getId())).findFirst().orElseThrow();
    ShippingAddress oldDefault = after.stream()
        .filter(a -> a.getId().equals(defaultAddr.getId())).findFirst().orElseThrow();

    assertThat(newDefault.isDefault()).isTrue();
    assertThat(oldDefault.isDefault()).isFalse();
  }

  @Test
  @DisplayName("update에서 isDefault=true 전달 시 기존 기본 배송지가 DB에서 해제된다")
  void update_withIsDefaultTrue_unsetsPreviousDefault_inDB() {
    // given: 배송지 2개 등록 (첫 번째가 기본)
    shippingAddressService.create(userId, createRequest("홍길동", "010-1234-5678", "06234",
        "서울시 강남구", null, null));
    shippingAddressService.create(userId, createRequest("김철수", "010-9999-8888", "12345",
        "부산시 해운대구", null, null));
    em.flush();
    em.clear();

    List<ShippingAddress> before = shippingAddressRepo.findByUserId(userId);
    ShippingAddress nonDefault = before.stream().filter(a -> !a.isDefault()).findFirst().orElseThrow();

    // when: 기본이 아닌 배송지를 update 시 isDefault=true 전달
    shippingAddressService.update(userId, nonDefault.getId(),
        new ShippingAddressUpdateRequest(null, null, null, null, null, null, true));
    em.flush();
    em.clear();

    // then: 기본 배송지가 정확히 1개이고, update한 배송지가 기본
    List<ShippingAddress> after = shippingAddressRepo.findByUserId(userId);
    long defaultCount = after.stream().filter(ShippingAddress::isDefault).count();
    assertThat(defaultCount).isEqualTo(1);
    assertThat(after.stream()
        .filter(a -> a.getId().equals(nonDefault.getId()))
        .findFirst().orElseThrow()
        .isDefault()).isTrue();
  }

  // ── 기본 배송지 삭제 제약 ────────────────────────────────────────────────────

  @Test
  @DisplayName("배송지가 여러 개일 때 기본 배송지 삭제 시 예외가 발생하고 DB에서 삭제되지 않는다")
  void delete_defaultAddress_withMultiple_throwsAndNotDeleted_inDB() {
    // given: 배송지 2개 등록 (첫 번째가 기본)
    shippingAddressService.create(userId, createRequest("홍길동", "010-1234-5678", "06234",
        "서울시 강남구", null, null));
    shippingAddressService.create(userId, createRequest("김철수", "010-9999-8888", "12345",
        "부산시 해운대구", null, null));
    em.flush();
    em.clear();

    List<ShippingAddress> addresses = shippingAddressRepo.findByUserId(userId);
    ShippingAddress defaultAddr = addresses.stream().filter(ShippingAddress::isDefault)
        .findFirst().orElseThrow();

    // when & then: 예외 발생
    assertThatThrownBy(() -> shippingAddressService.delete(userId, defaultAddr.getId()))
        .isInstanceOf(BusinessException.class)
        .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.SHIP_CANNOT_DELETE_DEFAULT));

    // then: DB에 여전히 존재
    em.flush();
    em.clear();
    assertThat(shippingAddressRepo.findByUserId(userId)).hasSize(2);
  }

  @Test
  @DisplayName("유일한 기본 배송지는 삭제할 수 있고 DB에서 제거된다")
  void delete_onlyAddress_isDeleted_fromDB() {
    // given: 배송지 1개 등록 (기본)
    shippingAddressService.create(userId, createRequest("홍길동", "010-1234-5678", "06234",
        "서울시 강남구", null, null));
    em.flush();
    em.clear();

    ShippingAddress address = shippingAddressRepo.findByUserId(userId).get(0);

    // when
    shippingAddressService.delete(userId, address.getId());
    em.flush();
    em.clear();

    // then
    assertThat(shippingAddressRepo.findByUserId(userId)).isEmpty();
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

  private User saveUser(String nickname) {
    Auth auth = Auth.builder().build();
    em.persist(auth);
    User u = User.builder()
        .nickname(nickname)
        .name("테스트")
        .phone(String.format("010-%04d-%04d", ++userSeq, userSeq))
        .gender(Gender.NONE)
        .auth(auth)
        .build();
    em.persist(u);
    return u;
  }

  private ShippingAddressCreateRequest createRequest(String name, String phone, String zipCode,
      String address1, String address2, String memo) {
    return new ShippingAddressCreateRequest(name, phone, zipCode, address1, address2, memo);
  }
}
