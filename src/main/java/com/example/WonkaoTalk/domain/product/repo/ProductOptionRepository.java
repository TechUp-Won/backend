package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

  List<ProductOption> findByProductOptionGroupId(Long productOptionGroupId);

  List<ProductOption> findByProductOptionGroupIdIn(List<Long> groupIds);
}
