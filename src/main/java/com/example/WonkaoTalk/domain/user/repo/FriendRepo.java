package com.example.WonkaoTalk.domain.user.repo;

import com.example.WonkaoTalk.domain.user.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRepo extends JpaRepository<Friend, Long> {

}
