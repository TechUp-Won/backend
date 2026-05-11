package com.example.WonkaoTalk.domain.store.repo;

import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.entity.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepo extends JpaRepository<Store, Long> {

  Optional<Store> findBySeller(Seller seller);

  boolean existsByName(String name);

  List<Store> findByNameContaining(String name);
}
