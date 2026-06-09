package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthLocalRepo extends JpaRepository<AuthLocal, Long> {

  Optional<AuthLocal> findByEmail(String email);

  Optional<AuthLocal> findByAuth(Auth auth);

  @Query("select al from AuthLocal al join fetch al.auth where al.email = :email")
  Optional<AuthLocal> findByEmailWithAuth(@Param("email") String email);

  boolean existsByEmail(String email);
}
