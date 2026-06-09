package com.example.WonkaoTalk.common.config.websocket;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
        StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    // 클라이언트가 STOMP 연결을 시도할 때
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
      String token = resolveToken(authorizationHeader);

      if (StringUtils.hasText(token)) {
        jwtTokenProvider.validateToken(token);

        Long userId = jwtTokenProvider.getUserId(token);

        if (accessor.getSessionAttributes() != null) {
          accessor.getSessionAttributes().put("userId", userId);
        }

        log.info("연결 성공: {}", userId);
      } else {
        log.error("연결 실패");
        throw new IllegalArgumentException("웹소켓 연결을 위한 토큰이 필요합니다.");
      }
    }
    return message;
  }


  private String resolveToken(String authorizationHeader) {
    if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }
    return null;
  }
}
