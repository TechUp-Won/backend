package com.example.WonkaoTalk.domain.friend.repo;

import com.example.WonkaoTalk.domain.friend.entity.Friend;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRepo extends JpaRepository<Friend, Long> {

  boolean existsByUserIdAndTargetId(Long userId, Long targetId);

  // 친구 목록 조회
  @Query("SELECT f FROM Friend f JOIN FETCH f.target WHERE f.user.id = :userId AND f.status = 'ACTIVE'")
  List<Friend> findAllActiveFriendsByUserId(@Param("userId") Long userId);

  Optional<Friend> findByIdAndUserId(Long id, Long userId);
}
