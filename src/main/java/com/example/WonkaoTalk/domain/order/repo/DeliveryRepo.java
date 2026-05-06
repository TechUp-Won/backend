package com.example.WonkaoTalk.domain.order.repo;

import com.example.WonkaoTalk.domain.order.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepo extends JpaRepository<Delivery, Long> {

}
