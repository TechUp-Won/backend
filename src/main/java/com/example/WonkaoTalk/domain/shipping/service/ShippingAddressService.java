package com.example.WonkaoTalk.domain.shipping.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShippingAddressService {

  private final ShippingAddressRepo shippingAddressRepo;
  private final UserRepo userRepo;

  @Transactional(readOnly = true)
  public ShippingAddressListResponse getList(Long userId) {
    List<ShippingAddressResponse> addresses = shippingAddressRepo.findByUserId(userId)
        .stream()
        .map(ShippingAddressResponse::from)
        .toList();
    return ShippingAddressListResponse.of(addresses);
  }

  @Transactional
  public ShippingAddressResponse create(Long userId, ShippingAddressCreateRequest request) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    boolean isFirst = shippingAddressRepo.countByUserId(userId) == 0;

    ShippingAddress address = ShippingAddress.builder()
        .user(user)
        .recipientName(request.recipientName())
        .recipientPhone(request.recipientPhone())
        .zipCode(request.zipCode())
        .address1(request.address1())
        .address2(request.address2())
        .memo(request.memo())
        .isDefault(isFirst)
        .build();

    return ShippingAddressResponse.from(shippingAddressRepo.save(address));
  }

  @Transactional
  public ShippingAddressResponse update(Long userId, Long shippingAddressId,
      ShippingAddressUpdateRequest request) {
    ShippingAddress address = shippingAddressRepo.findByIdAndUserId(shippingAddressId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SHIP_NOT_FOUND));

    address.update(request.recipientName(), request.recipientPhone(), request.zipCode(),
        request.address1(), request.address2(), request.memo());

    if (Boolean.TRUE.equals(request.isDefault())) {
      shippingAddressRepo.findDefaultByUserId(userId)
          .ifPresent(ShippingAddress::unsetDefault);
      address.setAsDefault();
    }

    return ShippingAddressResponse.from(address);
  }

  @Transactional
  public void delete(Long userId, Long shippingAddressId) {
    ShippingAddress address = shippingAddressRepo.findByIdAndUserId(shippingAddressId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SHIP_NOT_FOUND));

    if (address.isDefault() && shippingAddressRepo.countByUserId(userId) > 1) {
      throw new BusinessException(ErrorCode.SHIP_CANNOT_DELETE_DEFAULT);
    }

    shippingAddressRepo.delete(address);
  }

  @Transactional
  public ShippingAddressResponse setDefault(Long userId, Long shippingAddressId) {
    ShippingAddress address = shippingAddressRepo.findByIdAndUserId(shippingAddressId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SHIP_NOT_FOUND));

    shippingAddressRepo.findDefaultByUserId(userId)
        .ifPresent(ShippingAddress::unsetDefault);

    address.setAsDefault();

    return ShippingAddressResponse.from(address);
  }
}
