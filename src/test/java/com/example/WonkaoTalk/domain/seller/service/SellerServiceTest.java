package com.example.WonkaoTalk.domain.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.AuthIntegrationResultDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.seller.dto.SellerRegisterRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerResponse;
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerUpdateRequest;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
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
class SellerServiceTest {

  @InjectMocks
  private SellerService sellerService;

  @Mock
  private SellerRepo sellerRepo;
  @Mock
  private AuthRepo authRepo;
  @Mock
  private AuthService authService;

  private Seller seller;

  @BeforeEach
  void setUp() {
    seller = Seller.builder()
        .name("침착맨스토어")
        .phone("02-3456-7890")
        .buzNo("1234567890")
        .build();
    ReflectionTestUtils.setField(seller, "id", 1L);
  }

  @Test
  @DisplayName("기존 사용자의 판매자 등록 성공")
  public void registerSellerSuccess() {
    //given
    Long authId = 1L;
    SellerRegisterRequest request = SellerRegisterRequest.builder()
        .buzNo("1234567890")
        .name("판매자1")
        .phone("010-1234-5678")
        .build();

    Auth auth = Auth.builder()
        .id(authId)
        .role(Role.USER)
        .build();

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));
    given(sellerRepo.existsByBuzNo(request.buzNo())).willReturn(false);

    //when
    sellerService.registerSeller(authId, request);

    //then
    assertThat(auth.getRole()).isEqualTo(Role.USER_SELLER);
    verify(sellerRepo).save(any(Seller.class));
  }

  @Test
  @DisplayName("판매자 권한 승격 실패 - 사업자번호 이미 존재")
  public void registerSellerFailWhenDuplicateBuzNo() {
    // given
    SellerRegisterRequest request = new SellerRegisterRequest("1234567890", "중복스토어",
        "010-3333-3333");
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);

    given(sellerRepo.existsByBuzNo(request.buzNo())).willReturn(true);

    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.registerSeller(auth.getId(), request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELLER_DUPLICATE_BUZNO);
    verify(sellerRepo, never()).save(any());
  }

  @Test
  @DisplayName("신규 판매자 회원가입 성공")
  public void signUpAsSellerSuccess() {
    //given
    SellerSignUpRequest request = SellerSignUpRequest.builder()
        .email("test@gmail.com")
        .password("Qwer1234")
        .passwordCheck("Qwer1234")
        .buzNo("1234567890")
        .name("판매자1")
        .phone("010-1234-5678")
        .build();

    Auth auth = Auth.builder()
        .role(Role.SELLER)
        .build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthIntegrationResultDto resultDto = new AuthIntegrationResultDto(auth, true);

    given(sellerRepo.existsByBuzNo(request.buzNo())).willReturn(false);
    given(authService.linkOrCreateLocal(request.email(), request.password(), Role.SELLER))
        .willReturn(resultDto);

    //when
    sellerService.signUpAsSeller(request);

    //then
    verify(authService).linkOrCreateLocal(request.email(), request.password(), Role.SELLER);
    verify(sellerRepo).save(any(Seller.class));
  }

  @Test
  @DisplayName("판매자 회원가입 실패 - 비밀번호 불일치")
  public void signUpAsSellerFailWhenPasswordMismatch() {
    // given
    SellerSignUpRequest request = SellerSignUpRequest.builder()
        .email("test@gmail.com")
        .password("Qwer1234")
        .passwordCheck("DifferentPassword123!") // 불일치하는 비밀번호
        .buzNo("1234567890")
        .name("판매자1")
        .phone("010-1234-5678")
        .build();

    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.signUpAsSeller(request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_MISMATCH_PASSWORD);
  }

  @Test
  @DisplayName("판매자 회원가입 실패 - 사업자번호 중복")
  public void signUpAsSellerFailWhenDuplicateBuzNo() {
    // given
    SellerSignUpRequest request = SellerSignUpRequest.builder()
        .email("test@gmail.com")
        .password("Qwer1234")
        .passwordCheck("Qwer1234")
        .buzNo("1234567890")
        .name("판매자1")
        .phone("010-1234-5678")
        .build();

    given(sellerRepo.existsByBuzNo(request.buzNo())).willReturn(true);

    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.signUpAsSeller(request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELLER_DUPLICATE_BUZNO);
  }

  @Test
  @DisplayName("판매자 정보 조회 성공")
  public void getMySellerInfoSuccess() {
    //given
    given(sellerRepo.findById(1L)).willReturn(Optional.of(seller));

    //when
    SellerResponse response = sellerService.getSellerInfo(1L);

    //then
    assertThat(response.sellerId()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("침착맨스토어");
    assertThat(response.buzNo()).isEqualTo("1234567890");
  }

  @Test
  @DisplayName("판매자 정보 조회 실패 - sellerId가 null인 경우")
  public void getSellerInfoFailWhenIdIsNull() {
    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.getSellerInfo(null)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("판매자 정보 조회 실패 - 존재하지 않는 판매자")
  public void getSellerInfoFailWhenNotFound() {
    // given
    given(sellerRepo.findById(999L)).willReturn(Optional.empty());

    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.getSellerInfo(999L)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("판매자 정보 수정 성공")
  public void updateSellerInfoSuccess() {
    //given
    SellerUpdateRequest request = new SellerUpdateRequest("이병건스토어", "010-1111-1111");
    given(sellerRepo.findById(1L)).willReturn(Optional.of(seller));

    //when
    SellerResponse response = sellerService.updateSellerInfo(1L, request);

    //then
    assertThat(response.sellerId()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("이병건스토어");
    assertThat(response.phone()).isEqualTo("010-1111-1111");
  }

  @Test
  @DisplayName("판매자 정보 수정 성공 - 이름만 전달된 경우 (전화번호 기존 값 유지)")
  public void updateSellerInfoSuccessPartialNameOnly() {
    // given
    SellerUpdateRequest request = new SellerUpdateRequest("새로운스토어이름", null); // 전화번호 누락
    given(sellerRepo.findById(1L)).willReturn(Optional.of(seller));

    // when
    SellerResponse response = sellerService.updateSellerInfo(1L, request);

    // then
    assertThat(response.name()).isEqualTo("새로운스토어이름");
    assertThat(response.phone()).isEqualTo(seller.getPhone()); // 기존 번호가 유지되었는지 검증
  }

  @Test
  @DisplayName("판매자 정보 수정 성공 - 전화번호만 전달된 경우 (이름 기존 값 유지)")
  public void updateSellerInfoSuccessPartialPhoneOnly() {
    // given
    SellerUpdateRequest request = new SellerUpdateRequest("", "010-9999-9999"); // 이름 누락(빈 문자열)
    given(sellerRepo.findById(1L)).willReturn(Optional.of(seller));

    // when
    SellerResponse response = sellerService.updateSellerInfo(1L, request);

    // then
    assertThat(response.name()).isEqualTo(seller.getName()); // 기존 이름이 유지되었는지 검증
    assertThat(response.phone()).isEqualTo("010-9999-9999");
  }

  @Test
  @DisplayName("판매자 정보 수정 성공 - 빈 문자열 전달 시 기존 정보 유지")
  public void updateSellerInfoSuccessWhenEmptyString() {
    // given
    // 클라이언트가 고의 또는 실수로 공백이나 빈 문자열을 보낸 경우
    SellerUpdateRequest request = new SellerUpdateRequest("   ", "");
    given(sellerRepo.findById(1L)).willReturn(Optional.of(seller));

    // when
    SellerResponse response = sellerService.updateSellerInfo(1L, request);

    // then
    // 빈 문자열이 무시되고 기존 엔티티의 이름과 전화번호가 유지되었는지 객체 상태 검증
    assertThat(response.name()).isEqualTo(seller.getName());
    assertThat(response.phone()).isEqualTo(seller.getPhone());
  }

  @Test
  @DisplayName("판매자 정보 수정 실패 - sellerId가 null인 경우 방어 로직 작동")
  public void updateSellerInfoFailWhenIdIsNull() {
    // given
    SellerUpdateRequest request = new SellerUpdateRequest("수정스토어", "010-1234-5678");

    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.updateSellerInfo(null, request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("판매자 탈퇴 시 개인정보가 마스킹되고 Soft Delete 처리된다.")
  public void withdrawAndMaskSellerSuccess() {
    // given
    Seller seller = Seller.builder()
        .buzNo("1234567890")
        .name("판매자1")
        .phone("010-1234-5678")
        .build();
    ReflectionTestUtils.setField(seller, "id", 1L);
    given(sellerRepo.findByAuthId(1L)).willReturn(Optional.of(seller));

    // when
    sellerService.withdrawSeller(1L);

    // then
    then(sellerRepo).should(times(1)).findByAuthId(1L);

    assertThat(seller.getBuzNo()).isNotEqualTo("1234567890");
    assertThat(seller.getName()).isNotEqualTo("판매자1");
    assertThat(seller.getPhone()).isNotEqualTo("010-1234-5678");
  }

  @Test
  @DisplayName("판매자 탈퇴 실패 - Auth ID에 해당하는 판매자가 없는 경우")
  public void withdrawSellerFailWhenNotFound() {
    // given
    given(sellerRepo.findByAuthId(999L)).willReturn(Optional.empty());

    // when & then
    BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
        BusinessException.class,
        () -> sellerService.withdrawSeller(999L)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("활성 판매자 존재 여부 확인 - 존재하는 경우 true 반환")
  public void existsActiveSellerReturnsTrue() {
    // given
    given(sellerRepo.existsByAuthId(1L)).willReturn(true);

    // when
    boolean result = sellerService.existsActiveSeller(1L);

    // then
    assertThat(result).isTrue();
  }
}