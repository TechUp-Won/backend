package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.VariantOptionMap;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantOptionMapRepo extends JpaRepository<VariantOptionMap, Long> {

  List<VariantOptionMap> findByProductVariantId(Long variantId);

  List<VariantOptionMap> findByProductVariantIdIn(List<Long> variantIds);
}
