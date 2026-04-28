package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.VariantOptionMap;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantOptionMapRepository extends JpaRepository<VariantOptionMap, Long> {

  List<VariantOptionMap> findByProductVariant_Id(Long variantId);
}
