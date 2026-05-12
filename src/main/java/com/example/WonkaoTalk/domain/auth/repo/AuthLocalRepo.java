package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLocalRepo extends JpaRepository<AuthLocal, Long> {

  Optional<AuthLocal> findByEmail(String email);

  Optional<AuthLocal> findByAuth(Auth auth);

  boolean existsByEmail(String email);
}
