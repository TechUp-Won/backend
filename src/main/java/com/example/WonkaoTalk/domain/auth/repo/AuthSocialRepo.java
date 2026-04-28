package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSocialRepo extends JpaRepository<AuthSocial, Long> {

}
