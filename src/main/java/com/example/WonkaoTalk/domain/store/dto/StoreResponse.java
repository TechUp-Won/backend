package com.example.WonkaoTalk.domain.store.dto;

import com.example.WonkaoTalk.domain.store.entity.Store;
import lombok.Builder;

@Builder
public record StoreResponse(
    Long id,
    Long sellerId,
    String name,
    String description,
    String phone,
    String thumbnail
) {

  public static StoreResponse from(Store store) {
    return StoreResponse.builder()
        .id(store.getId())
        .sellerId(store.getSeller().getId())
        .name(store.getName())
        .description(store.getDescription())
        .phone(store.getPhone())
        .thumbnail(store.getThumbnail())
        .build();
  }

}
