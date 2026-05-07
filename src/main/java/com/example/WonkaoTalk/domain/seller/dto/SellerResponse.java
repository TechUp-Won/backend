package com.example.WonkaoTalk.domain.seller.dto;

import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import lombok.Builder;

@Builder
public record SellerResponse(
    Long sellerId,
    String buzNo,
    String name,
    String phone,
    Role currentRole
) {

  public static SellerResponse of(Seller seller, Role currentRole) {
    return SellerResponse.builder()
        .sellerId(seller.getId())
        .buzNo(seller.getBuzNo())
        .name(seller.getName())
        .phone(seller.getPhone())
        .currentRole(currentRole)
        .build();
  }

}
