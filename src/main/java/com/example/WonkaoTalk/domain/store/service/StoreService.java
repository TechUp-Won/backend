package com.example.WonkaoTalk.domain.store.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.dto.StoreCreateRequest;
import com.example.WonkaoTalk.domain.store.dto.StoreResponse;
import com.example.WonkaoTalk.domain.store.dto.StoreUpdateRequest;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

  private final StoreRepo storeRepo;

  @Transactional
  public StoreResponse createStore(Seller seller, StoreCreateRequest request) {
    storeRepo.findBySeller(seller).ifPresent(s -> {
      throw new BusinessException(ErrorCode.STORE_EXISTS_ALREADY);
    });

    if (storeRepo.existsByName(request.name())) {
      throw new BusinessException(ErrorCode.STORE_EXISTS_NAME);
    }

    Store store = Store.builder()
        .name(request.name())
        .description(request.description())
        .phone(request.phone())
        .seller(seller)
        .build();

    Store savedStore = storeRepo.save(store);
    return StoreResponse.from(savedStore);
  }

  @Transactional
  public StoreResponse updateStore(Seller seller, StoreUpdateRequest request) {
    Store store = storeRepo.findBySeller(seller)
        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

    store.updateInfo(request.name(), request.description(), request.phone(), request.thumbnail());
    return StoreResponse.from(store);
  }

  @Transactional
  public void deleteStore(Seller seller) {
    Store store = storeRepo.findBySeller(seller)
        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

    store.deleteStore();
  }
}
