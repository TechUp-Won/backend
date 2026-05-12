package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepo extends JpaRepository<Category, Long> {

  List<Category> findByParentCategoryId(Long parentId);

  @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parentCategory")
  List<Category> findAllWithParent();
}
