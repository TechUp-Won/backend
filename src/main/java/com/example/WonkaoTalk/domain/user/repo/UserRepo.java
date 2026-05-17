package com.example.WonkaoTalk.domain.user.repo;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepo extends JpaRepository<User, Long> {

  Optional<User> findByAuth(Auth auth);

  Optional<User> findByAuthId(Long authId);

  Optional<User> findByPhone(String phone);

  boolean existsByAuthId(Long authId);

  boolean existByPhone(String phone);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :userId")
  Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
