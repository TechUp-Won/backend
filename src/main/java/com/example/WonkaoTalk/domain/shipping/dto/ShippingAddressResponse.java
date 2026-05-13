package com.example.WonkaoTalk.domain.shipping.dto;

import com.example.WonkaoTalk.domain.shipping.entity.ShippingAddress;
import lombok.Builder;

@Builder
public record ShippingAddressResponse(
    Long shippingAddressId,
    String recipientName,
    String recipientPhone,
    String zipCode,
    String address1,
    String address2,
    boolean isDefault,
    String memo
) {

  public static ShippingAddressResponse from(ShippingAddress address) {
    return ShippingAddressResponse.builder()
        .shippingAddressId(address.getId())
        .recipientName(address.getRecipientName())
        .recipientPhone(address.getRecipientPhone())
        .zipCode(address.getZipCode())
        .address1(address.getAddress1())
        .address2(address.getAddress2())
        .isDefault(address.isDefault())
        .memo(address.getMemo())
        .build();
  }
}
