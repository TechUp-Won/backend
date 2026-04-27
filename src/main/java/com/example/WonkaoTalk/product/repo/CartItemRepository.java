package com.example.WonkaoTalk.product.repo;

import com.example.WonkaoTalk.product.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
