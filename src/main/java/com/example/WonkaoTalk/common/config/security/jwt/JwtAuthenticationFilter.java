package com.example.WonkaoTalk.common.config.security.jwt;

import com.example.WonkaoTalk.common.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisService redisService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String token = resolveToken(request);

    if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
      // 토큰 블랙리스트 체크
      if (redisService.hasKey("BlackList:" + token)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"ERROR\", \"message\":\"이미 로그아웃된 토큰입니다.\"}");
        return;
      }

      String email = jwtTokenProvider.getEmailFromToken(token);
      String role = jwtTokenProvider.getRoleFromToken(token);

      // 시큐리티 권한 객체로 반환
      List<GrantedAuthority> authorities = Collections.singletonList(
          new SimpleGrantedAuthority("ROLE_" + role)
      );

      // 시큐리티 인증 객체 생성
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(email, null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");
    if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer")) {
      return bearer.substring(7);
    }
    return null;
  }

}
