package com.example.WonkaoTalk.domain.seller.dto;

import com.example.WonkaoTalk.domain.seller.entity.Seller;
import lombok.Builder;

@Builder
public record SellerResponse(
    Long sellerId,
    String name,
    String phone,
    String buzNo
) {

  public static SellerResponse from(Seller seller) {
    return SellerResponse.builder()
        .sellerId(seller.getId())
        .name(seller.getName())
        .phone(seller.getPhone())
        .buzNo(seller.getBuzNo())
        .build();
  }
}
