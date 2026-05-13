package com.example.WonkaoTalk.domain.shipping.repo;

import com.example.WonkaoTalk.domain.shipping.entity.ShippingAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShippingAddressRepo extends JpaRepository<ShippingAddress, Long> {

  List<ShippingAddress> findByUserId(Long userId);

  Optional<ShippingAddress> findByIdAndUserId(Long id, Long userId);

  long countByUserId(Long userId);

  @Query("SELECT s FROM ShippingAddress s WHERE s.user.id = :userId AND s.isDefault = true")
  Optional<ShippingAddress> findDefaultByUserId(@Param("userId") Long userId);
}
