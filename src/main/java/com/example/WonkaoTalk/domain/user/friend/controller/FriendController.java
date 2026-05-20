package com.example.WonkaoTalk.domain.user.friend.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendAddRequest;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendAddResponse;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendInfoDTO;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendListResponse;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendStatusRequest;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendUpdateRequest;
import com.example.WonkaoTalk.domain.user.friend.dto.FriendUpdateResponse;
import com.example.WonkaoTalk.domain.user.friend.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "친구", description = "친구 추가, 조회, 수정, 상태 변경, 삭제 API")
public class FriendController {

  private final FriendService friendService;

  @Operation(summary = "친구 추가", description = "사용자를 친구 목록에 추가합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<FriendAddResponse>> addFriend(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody FriendAddRequest request
  ) {
    FriendAddResponse response = friendService.addFriend(userDetails.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("친구 추가가 완료되었습니다.", response));
  }

  @Operation(summary = "친구 목록 조회", description = "로그인한 사용자의 친구 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<FriendListResponse>> getFriends(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    List<FriendInfoDTO> friendInfos = friendService.getFriends(userDetails.getUserId())
        .stream()
        .map(FriendInfoDTO::from)
        .toList();

    FriendListResponse response = FriendListResponse.of(friendInfos);
    return ResponseEntity.ok(ApiResponse.success("친구 조회를 완료했습니다.", response));
  }

  @Operation(summary = "친구 정보 수정", description = "친구의 별칭 등 친구 정보를 수정합니다.")
  @PatchMapping("/{friendId}")
  public ResponseEntity<ApiResponse<FriendUpdateResponse>> updateFriendInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long friendId,
      @Valid @RequestBody FriendUpdateRequest request
  ) {
    FriendUpdateResponse response = friendService.updateFriendsInfo(
        userDetails.getUserId(), friendId, request);
    return ResponseEntity.ok(ApiResponse.success("친구 정보가 수정되었습니다.", response));
  }

  @Operation(summary = "친구 상태 변경", description = "친구 요청 수락, 거절 등 친구 상태를 변경합니다.")
  @PatchMapping("/{friendId}/status")
  public ResponseEntity<ApiResponse<Void>> changeFriendStatus(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long friendId,
      @Valid @RequestBody FriendStatusRequest request
  ) {
    friendService.changeFriendStatus(userDetails.getUserId(), friendId, request);
    return ResponseEntity.ok(ApiResponse.success("친구 상태가 변경 되었습니다.", null));
  }

  @Operation(summary = "친구 삭제", description = "친구 목록에서 선택한 친구를 삭제합니다.")
  @DeleteMapping("/{friendId}")
  public ResponseEntity<ApiResponse<Void>> deleteFriend(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long friendId
  ) {
    friendService.deleteFriend(userDetails.getUserId(), friendId);
    return ResponseEntity.ok(ApiResponse.success("친구가 삭제 되었습니다.", null));
  }

}
