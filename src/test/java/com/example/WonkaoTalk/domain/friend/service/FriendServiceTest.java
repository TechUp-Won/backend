package com.example.WonkaoTalk.domain.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.friend.dto.FriendAddRequest;
import com.example.WonkaoTalk.domain.friend.dto.FriendAddResponse;
import com.example.WonkaoTalk.domain.friend.dto.FriendStatusRequest;
import com.example.WonkaoTalk.domain.friend.dto.FriendUpdateRequest;
import com.example.WonkaoTalk.domain.friend.dto.FriendUpdateResponse;
import com.example.WonkaoTalk.domain.friend.entity.Friend;
import com.example.WonkaoTalk.domain.friend.enums.FriendStatus;
import com.example.WonkaoTalk.domain.friend.repo.FriendRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

  @InjectMocks
  private FriendService friendService;

  @Mock
  private FriendRepo friendRepo;

  @Mock
  private UserRepo userRepo;

  private User me;
  private User target;
  private Friend friend;

  @BeforeEach
  void setUp() {
    me = User.builder().nickname("본인").name("나").phone("010-1111-1111").build();
    ReflectionTestUtils.setField(me, "id", 1L);

    target = User.builder().nickname("목표").name("너").phone("010-2222-2222").build();
    ReflectionTestUtils.setField(target, "id", 2L);

    friend = Friend.builder()
        .user(me)
        .target(target)
        .alias("별명")
        .build();
    ReflectionTestUtils.setField(friend, "id", 100L);
    ReflectionTestUtils.setField(friend, "status", FriendStatus.ACTIVE);
  }

  @Test
  @DisplayName("친구 추가 성공")
  public void addFriendSuccess() {
    //given
    FriendAddRequest request = new FriendAddRequest(2L);

    given(friendRepo.existsByUserIdAndTargetId(1L, 2L)).willReturn(false);
    given(userRepo.findById(1L)).willReturn(Optional.of(me));
    given(userRepo.findById(2L)).willReturn(Optional.of(target));
    given(friendRepo.save(any(Friend.class))).willReturn(friend);

    //when
    FriendAddResponse response = friendService.addFriend(1L, request);

    //then
    assertThat(response).isNotNull();
    assertThat(response.friendId()).isEqualTo(100L);
    verify(friendRepo).save(any(Friend.class));
  }

  @Test
  @DisplayName("친구 추가 실패 - 자기 자신을 추가")
  void addFriendFailSelfRef() {
    FriendAddRequest request = new FriendAddRequest(1L);

    BusinessException exception = assertThrows(BusinessException.class, () ->
        friendService.addFriend(1L, request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FRND_SELF_REF);
  }

  @Test
  @DisplayName("친구 추가 실패 - 이미 등록된 친구")
  void addFriendFailAlreadyExists() {
    FriendAddRequest request = new FriendAddRequest(2L);

    given(friendRepo.existsByUserIdAndTargetId(1L, 2L)).willReturn(true);

    BusinessException exception = assertThrows(BusinessException.class, () ->
        friendService.addFriend(1L, request)
    );
    assertThat(exception.getErrorCode().name()).contains("FRND_REGISTERED_ALREADY");
  }

  @Test
  @DisplayName("친구 추가 실패 - 요청자 ID(userId)가 null인 경우")
  void addFriendFailUserIdIsNull() {
    // given
    FriendAddRequest request = new FriendAddRequest(2L);

    // when & then
    BusinessException exception = assertThrows(BusinessException.class, () ->
        friendService.addFriend(null, request)
    );

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FRND_SELF_REF);
  }

  @Test
  @DisplayName("친구 목록 조회 성공")
  public void getFriendsSuccess() {
    //given
    given(friendRepo.findAllActiveFriendsByUserId(1L)).willReturn(List.of(friend));

    //when
    List<Friend> friends = friendService.getFriends(1L);

    //then
    assertThat(friends).hasSize(1);
    assertThat(friends.getFirst().getId()).isEqualTo(100L);
  }

  @Test
  @DisplayName("친구 정보 변경 성공 - 별명 및 메모 변경")
  public void updateFriendSuccess() {
    //given
    FriendUpdateRequest request = new FriendUpdateRequest("새별명", "새메모");
    given(friendRepo.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(friend));

    //when
    FriendUpdateResponse response = friendService.updateFriendsInfo(1L, 100L, request);

    //then
    assertThat(friend.getAlias()).isEqualTo("새별명");
    assertThat(friend.getMemo()).isEqualTo("새메모");
    assertThat(response).isNotNull();
  }

  @Test
  @DisplayName("친구 정보 변경 성공 - 별명만 변경하고 메모는 null로 요청 (부분 업데이트)")
  void updateFriendPartialOnlyAlias() {
    // given
    // memo를 null로 전달
    FriendUpdateRequest request = new FriendUpdateRequest("바뀐별명", null);
    friend.updateMemo("기존메모"); // 사전에 기존 메모 세팅
    given(friendRepo.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(friend));

    // when
    FriendUpdateResponse response = friendService.updateFriendsInfo(1L, 100L, request);

    // then
    assertThat(friend.getAlias()).isEqualTo("바뀐별명"); // 별명은 변경됨
    assertThat(friend.getMemo()).isEqualTo("기존메모"); // 메모는 기존 값 유지됨
  }

  @Test
  @DisplayName("친구 정보 변경 성공 - 메모만 변경하고 별명은 null로 요청 (부분 업데이트)")
  void updateFriendPartialOnlyMemo() {
    // given
    // alias를 null로 전달
    FriendUpdateRequest request = new FriendUpdateRequest(null, "바뀐메모");
    String originalAlias = friend.getAlias();
    given(friendRepo.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(friend));

    // when
    FriendUpdateResponse response = friendService.updateFriendsInfo(1L, 100L, request);

    // then
    assertThat(friend.getAlias()).isEqualTo(originalAlias); // 별명은 기존 값 유지됨
    assertThat(friend.getMemo()).isEqualTo("바뀐메모"); // 메모는 변경됨
  }

  @Test
  @DisplayName("친구 정보 변경 실패 - 존재하지 않는 친구 관계")
  void updateFriendFailNotFound() {
    FriendUpdateRequest request = new FriendUpdateRequest("새별명", "새메모");
    given(friendRepo.findByIdAndUserId(100L, 1L)).willReturn(Optional.empty());

    BusinessException exception = assertThrows(BusinessException.class, () ->
        friendService.updateFriendsInfo(1L, 100L, request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FRND_NOT_FOUND);
  }

  @Test
  @DisplayName("친구 상태 변경 성공")
  void changeFriendStatusSuccess() {
    FriendStatusRequest request = new FriendStatusRequest(FriendStatus.HIDDEN);
    given(friendRepo.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(friend));

    friendService.changeFriendStatus(1L, 100L, request);

    assertThat(friend.getStatus()).isEqualTo(FriendStatus.HIDDEN);
  }

  @Test
  @DisplayName("친구 삭제 성공")
  public void deleteFriendSuccess() {
    //given
    given(friendRepo.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(friend));

    //when
    friendService.deleteFriend(1L, 100L);

    //then
    verify(friendRepo).delete(friend);
  }

}