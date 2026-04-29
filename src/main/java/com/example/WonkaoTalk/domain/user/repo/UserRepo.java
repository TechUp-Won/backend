package com.example.WonkaoTalk.domain.user.repo;

import com.example.WonkaoTalk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {

}
