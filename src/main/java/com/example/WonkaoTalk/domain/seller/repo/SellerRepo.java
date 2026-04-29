package com.example.WonkaoTalk.domain.seller.repo;

import com.example.WonkaoTalk.domain.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepo extends JpaRepository<Seller, Long> {

}
