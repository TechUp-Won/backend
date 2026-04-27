package com.example.WonkaoTalk.product.repo;

import com.example.WonkaoTalk.product.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
