package com.example.WonkaoTalk.domain.user.repo;

import com.example.WonkaoTalk.domain.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepo extends JpaRepository<UserConsent, Long> {

}
