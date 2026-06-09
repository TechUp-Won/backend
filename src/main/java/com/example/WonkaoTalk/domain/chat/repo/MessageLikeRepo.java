package com.example.WonkaoTalk.domain.chat.repo;

import com.example.WonkaoTalk.domain.chat.entity.MessageLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLikeRepo extends JpaRepository<MessageLike, Long> {

}
