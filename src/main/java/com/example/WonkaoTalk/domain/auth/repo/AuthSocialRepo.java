package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSocialRepo extends JpaRepository<AuthSocial, Long> {

  Optional<AuthSocial> findFirstByEmail(String email);

  List<AuthSocial> findByAuth(Auth auth);

  List<AuthSocial> findByAuthId(Long authId);

  Optional<AuthSocial> findByProviderAndProviderUserId(AuthProvider provider, String providerId);

  Optional<AuthSocial> findByProviderUserId(String providerId);

  @Query("SELECT s FROM AuthSocial s JOIN FETCH s.auth WHERE s.provider = :provider AND s.providerUserId = :providerUserId")
  Optional<AuthSocial> findByProviderAndProviderUserIdWithAuth(
      @Param("provider") AuthProvider provider,
      @Param("providerUserId") String providerUserId
  );
}
