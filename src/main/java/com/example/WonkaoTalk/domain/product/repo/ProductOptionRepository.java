package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.ProductOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

  List<ProductOption> findByProductOptionGroup_Id(Long productOptionGroupId);

  List<ProductOption> findByProductOptionGroup_IdIn(List<Long> groupIds);
}
