package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepo extends JpaRepository<Auth, Long> {

}
