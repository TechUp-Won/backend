package com.example.WonkaoTalk.product.repo;

import com.example.WonkaoTalk.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
