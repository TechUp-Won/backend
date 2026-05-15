package com.example.WonkaoTalk.domain.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
        .id(1L)
        .role(Role.SELLER)
        .build();

    given(sellerRepo.existsByBuzNo(request.buzNo())).willReturn(false);
    given(authService.createAuthLocal(request.email(), request.password(), Role.SELLER))
        .willReturn(auth);

    //when
    sellerService.signUpAsSeller(request);

    //then
    verify(authService).createAuthLocal(request.email(), request.password(), Role.SELLER);
    verify(sellerRepo).save(any(Seller.class));

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
}