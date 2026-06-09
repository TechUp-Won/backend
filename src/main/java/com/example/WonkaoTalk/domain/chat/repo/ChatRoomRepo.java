package com.example.WonkaoTalk.domain.chat.repo;

import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepo extends JpaRepository<ChatRoom, Long> {

}
