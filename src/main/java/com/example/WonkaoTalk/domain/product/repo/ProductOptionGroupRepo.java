package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductOptionGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionGroupRepo extends JpaRepository<ProductOptionGroup, Long> {

  List<ProductOptionGroup> findByProductId(Long productId);
}
