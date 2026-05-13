package com.example.WonkaoTalk.domain.user.friend.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendRequest;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendResponse;
import com.example.WonkaoTalk.domain.user.friend.service.FriendService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
public class FriendController {

  private final FriendService friendService;

  @PostMapping
  public ResponseEntity<ApiResponse<FriendResponse.AddResponse>> addFriend(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody FriendRequest.AddRequest request
  ) {
    FriendResponse.AddResponse response = friendService.addFriend(userDetails.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("친구 추가가 완료되었습니다.", response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<FriendResponse.FriendListResponse>> getFriends(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    List<FriendResponse.Info> friendInfos = friendService.getFriends(userDetails.getUserId())
        .stream()
        .map(FriendResponse.Info::from)
        .toList();

    FriendResponse.FriendListResponse response = FriendResponse.FriendListResponse.of(friendInfos);
    return ResponseEntity.ok(ApiResponse.success("친구 조회를 완료했습니다.", response));
  }

  @PatchMapping("/{friendId}")
  public ResponseEntity<ApiResponse<FriendResponse.UpdateResponse>> updateFriendInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long friendId,
      @Valid @RequestBody FriendRequest.UpdateRequest request
  ) {
    FriendResponse.UpdateResponse response = friendService.updateFriendsInfo(
        userDetails.getUserId(), friendId, request);
    return ResponseEntity.ok(ApiResponse.success("친구 정보가 수정되었습니다.", response));
  }

  @PatchMapping("/{friendId}/status")
  public ResponseEntity<ApiResponse<Void>> changeFriendStatus(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long friendId,
      @Valid @RequestBody FriendRequest.StatusRequest request
  ) {
    friendService.changeFriendStatus(userDetails.getUserId(), friendId, request);
    return ResponseEntity.ok(ApiResponse.success("친구 상태가 변경 되었습니다.", null));
  }

  @DeleteMapping("/{friendId}")
  public ResponseEntity<ApiResponse<Void>> deleteFriend(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long friendId
  ) {
    friendService.deleteFriend(userDetails.getUserId(), friendId);
    return ResponseEntity.ok(ApiResponse.success("친구가 삭제 되었습니다.", null));
  }

}
