package com.example.WonkaoTalk.product.repo;

import com.example.WonkaoTalk.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
}
