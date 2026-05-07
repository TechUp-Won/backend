package com.example.WonkaoTalk.domain.seller.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.seller.dto.SellerRegisterRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpResponse;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {

  private final AuthService authService;
  private final AuthRepo authRepo;
  private final SellerRepo sellerRepo;

  @Transactional
  public SellerSignUpResponse signUpAsSeller(SellerSignUpRequest request) {
    if (sellerRepo.existsByBuzNo(request.buzNo())) {
      throw new BusinessException(ErrorCode.SELLER_DUPLICATE_BUZNO);
    }

    Auth savedAuth = authService.createAuthLocal(request.email(), request.password(), Role.SELLER);

    Seller seller = Seller.builder()
        .auth(savedAuth)
        .buzNo(request.buzNo())
        .name(request.name())
        .phone(request.phone())
        .build();

    sellerRepo.save(seller);

    return SellerSignUpResponse.of(seller, savedAuth.getRole());
  }

  @Transactional
  public SellerSignUpResponse registerSeller(Long authId, SellerRegisterRequest request) {
    if (sellerRepo.existsByBuzNo(request.buzNo())) {
      throw new BusinessException(ErrorCode.SELLER_DUPLICATE_BUZNO);
    }

    Auth auth = authRepo.findById(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    if (auth.getRole() != Role.USER) {
      throw new BusinessException(ErrorCode.SELLER_REGISTERED_ACCOUNT);
    }

    auth.updateRole(Role.USER_SELLER);

    Seller seller = Seller.builder()
        .auth(auth)
        .buzNo(request.buzNo())
        .name(request.name())
        .phone(request.phone())
        .build();

    sellerRepo.save(seller);

    return SellerSignUpResponse.of(seller, auth.getRole());
  }

}
