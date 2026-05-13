package com.example.WonkaoTalk.domain.shipping.repo;

import com.example.WonkaoTalk.domain.shipping.entity.ShippingAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingAddressRepo extends JpaRepository<ShippingAddress, Long> {

  List<ShippingAddress> findByUserId(Long userId);
}
