package com.example.WonkaoTalk.domain.user.friend.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendRequest;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendResponse;
import com.example.WonkaoTalk.domain.user.friend.entity.Friend;
import com.example.WonkaoTalk.domain.user.friend.repo.FriendRepo;
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
  public FriendResponse.AddResponse addFriend(Long userId, FriendRequest.AddRequest request) {
    if (userId.equals(request.targetId())) {
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
    return FriendResponse.AddResponse.of(savedFriend.getId());
  }

  @Transactional
  public List<Friend> getFriends(Long userId) {
    return friendRepo.findAllActiveFriendsByUserId(userId);
  }

  @Transactional
  public FriendResponse.UpdateResponse updateFriendsInfo(Long userId, Long friendId,
      FriendRequest.UpdateRequest request) {
    Friend friend = friendRepo.findByIdAndUserId(friendId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.FRND_NOT_FOUND));
    friend.updateAlias(request.alias());
    friend.updateMemo(request.memo());

    return FriendResponse.UpdateResponse.from(friend);
  }

  @Transactional
  public void changeFriendStatus(Long userId, Long friendId, FriendRequest.StatusRequest request) {
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

