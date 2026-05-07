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
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpRequest;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @Test
  @DisplayName("기존 사용자의 판매자 등록 성공")
  public void registerSellerSuccess() {
    //given
    Long authId = 1L;
    SellerRegisterRequest request = SellerRegisterRequest.builder()
        .buzNo("1234567890")
        .name("Store1")
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
        .buzNo("1234567890")
        .name("Store1")
        .phone("010-1234-5678")
        .build();

    Auth auth = Auth.builder()
        .id(1L)
        .role(Role.SELLER)
        .build();

    given(sellerRepo.existsByBuzNo(request.buzNo())).willReturn(false);
    given(authService.createAuth(request.email(), request.password(), Role.SELLER))
        .willReturn(auth);

    //when
    sellerService.signUpAsSeller(request);

    //then
    verify(authService).createAuth(request.email(), request.password(), Role.SELLER);
    verify(sellerRepo).save(any(Seller.class));

  }

}