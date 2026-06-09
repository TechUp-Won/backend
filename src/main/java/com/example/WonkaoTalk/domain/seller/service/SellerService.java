package com.example.WonkaoTalk.domain.seller.service;

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
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpResponse;
import com.example.WonkaoTalk.domain.seller.dto.SellerUpdateRequest;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {

  private final AuthService authService;
  private final AuthRepo authRepo; //TODO: authRepo 의존성 제거
  private final SellerRepo sellerRepo;

  @Transactional
  public SellerSignUpResponse signUpAsSeller(SellerSignUpRequest request) {
    if (!request.password().equals(request.passwordCheck())) {
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    if (sellerRepo.existsByBuzNo(request.buzNo())) {
      throw new BusinessException(ErrorCode.SELLER_DUPLICATE_BUZNO);
    }

    AuthIntegrationResultDto result = authService.linkOrCreateLocal(request.email(),
        request.password(), Role.SELLER);
    Auth auth = result.auth();
    Seller seller;

    if (result.isNewCreated()) {
      seller = Seller.builder()
          .auth(auth)
          .buzNo(request.buzNo())
          .name(request.name())
          .phone(request.phone())
          .build();
      sellerRepo.save(seller);
    } else {
      if (auth.getRole() == Role.USER) {
        auth.updateRole(Role.USER_SELLER);
      }
      seller = sellerRepo.findByAuth(auth).orElseGet(() -> {
        Seller newSeller = Seller.builder()
            .auth(auth)
            .buzNo(request.buzNo())
            .name(request.name())
            .phone(request.phone())
            .build();
        return sellerRepo.save(newSeller);
      });
    }
    return SellerSignUpResponse.of(seller, auth.getRole());
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

  @Transactional(readOnly = true)
  public SellerResponse getSellerInfo(Long sellerId) {
    if (sellerId == null) {
      throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
    }
    Seller seller = findSeller(sellerId);

    return SellerResponse.from(seller);
  }

  @Transactional
  public SellerResponse updateSellerInfo(Long sellerId, SellerUpdateRequest request) {
    if (sellerId == null) {
      throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
    }
    Seller seller = findSeller(sellerId);

    String name = StringUtils.hasText(request.name()) ? request.name() : seller.getName();
    String phone = StringUtils.hasText(request.phone()) ? request.phone() : seller.getPhone();

    seller.update(name, phone);

    return SellerResponse.from(seller);
  }

  @Transactional
  public void withdrawSeller(Long authId) {
    Seller seller = sellerRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

    seller.withdraw();
  }

  @Transactional(readOnly = true)
  public boolean existsActiveSeller(Long authId) {
    return sellerRepo.existsByAuthId(authId);
  }

  @Transactional(readOnly = true)
  public Seller findSeller(Long sellerId) {
    return sellerRepo.findById(sellerId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
  }
}
