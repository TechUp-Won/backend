package com.example.WonkaoTalk.domain.store.service;

import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

  private final StoreRepo storeRepo;


}
