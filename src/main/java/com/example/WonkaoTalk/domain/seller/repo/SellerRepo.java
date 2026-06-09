package com.example.WonkaoTalk.domain.seller.repo;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepo extends JpaRepository<Seller, Long> {

  boolean existsByBuzNo(String buzNo);

  Optional<Seller> findByAuth(Auth auth);

  Optional<Seller> findByAuthId(Long authId);

  boolean existsByAuthId(Long authId);
}
