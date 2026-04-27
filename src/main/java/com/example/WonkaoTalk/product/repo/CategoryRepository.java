package com.example.WonkaoTalk.product.repo;

import com.example.WonkaoTalk.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
