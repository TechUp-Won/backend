package com.example.WonkaoTalk.domain.friend.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.friend.dto.FriendAddRequest;
import com.example.WonkaoTalk.domain.friend.dto.FriendAddResponse;
import com.example.WonkaoTalk.domain.friend.dto.FriendStatusRequest;
import com.example.WonkaoTalk.domain.friend.dto.FriendUpdateRequest;
import com.example.WonkaoTalk.domain.friend.dto.FriendUpdateResponse;
import com.example.WonkaoTalk.domain.friend.entity.Friend;
import com.example.WonkaoTalk.domain.friend.repo.FriendRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

  private final FriendRepo friendRepo;
  private final UserRepo userRepo;

  @Transactional
  public FriendAddResponse addFriend(Long userId, FriendAddRequest request) {
    if (userId == null || userId.equals(request.targetId())) {
      throw new BusinessException(ErrorCode.FRND_SELF_REF);
    }

    if (friendRepo.existsByUserIdAndTargetId(userId, request.targetId())) {
      throw new BusinessException(ErrorCode.FRND_REGISTERED_ALREADY);
    }

    User me = userRepo.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    User target = userRepo.findById(request.targetId())
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Friend newFriend = Friend.builder()
        .user(me)
        .target(target)
        .alias(target.getName())
        .build();

    Friend savedFriend = friendRepo.save(newFriend);
    return FriendAddResponse.of(savedFriend.getId());
  }

  @Transactional(readOnly = true)
  public List<Friend> getFriends(Long userId) {
    return friendRepo.findAllActiveFriendsByUserId(userId);
  }

  @Transactional
  public FriendUpdateResponse updateFriendsInfo(Long userId, Long friendId,
      FriendUpdateRequest request) {
    Friend friend = friendRepo.findByIdAndUserId(friendId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.FRND_NOT_FOUND));
    if (request.alias() != null) {
      friend.updateAlias(request.alias());
    }
    if (request.memo() != null) {
      friend.updateMemo(request.memo());
    }

    return FriendUpdateResponse.from(friend);
  }

  @Transactional
  public void changeFriendStatus(Long userId, Long friendId, FriendStatusRequest request) {
    Friend friend = friendRepo.findByIdAndUserId(friendId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.FRND_NOT_FOUND));

    friend.changeStatus(request.status());
  }

  @Transactional
  public void deleteFriend(Long userId, Long friendId) {
    Friend friend = friendRepo.findByIdAndUserId(friendId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.FRND_NOT_FOUND));

    friendRepo.delete(friend);
  }
}