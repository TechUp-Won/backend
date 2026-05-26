package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSocialRepo extends JpaRepository<AuthSocial, Long> {

  Optional<AuthSocial> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
