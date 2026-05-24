package com.example.WonkaoTalk.domain.product.repo;

import com.example.WonkaoTalk.domain.product.entity.DeletedProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedProductImageRepo extends JpaRepository<DeletedProductImage, Long> {

}
