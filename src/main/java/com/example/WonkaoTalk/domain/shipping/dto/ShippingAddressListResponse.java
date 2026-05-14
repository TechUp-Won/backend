package com.example.WonkaoTalk.domain.shipping.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ShippingAddressListResponse(
    List<ShippingAddressResponse> addresses
) {

  public static ShippingAddressListResponse of(List<ShippingAddressResponse> addresses) {
    return ShippingAddressListResponse.builder()
        .addresses(addresses)
        .build();
  }
}
