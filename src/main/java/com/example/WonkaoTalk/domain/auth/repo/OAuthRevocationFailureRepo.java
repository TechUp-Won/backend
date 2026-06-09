package com.example.WonkaoTalk.domain.auth.repo;

import com.example.WonkaoTalk.domain.auth.entity.OAuthRevocationFailure;
import com.example.WonkaoTalk.domain.auth.enums.FallbackStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthRevocationFailureRepo extends JpaRepository<OAuthRevocationFailure, Long> {

  List<OAuthRevocationFailure> findTop50ByStatusOrderByCreatedAtAsc(FallbackStatus status);

}
