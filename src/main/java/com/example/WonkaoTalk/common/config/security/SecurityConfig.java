package com.example.WonkaoTalk.common.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // REST API 서버이므로 CSRF 보호 비활성화 (토큰 기반 인증을 사용할 예정이므로 불필요)
        .csrf(AbstractHttpConfigurer::disable)

        // HTTP 요청에 대한 접근 권한 설정
        .authorizeHttpRequests(auth -> auth
            // 명시한 엔드포인트만 인증 없이 접근 허용
            .requestMatchers(
                "/api/v1/auth/check-email",
                "/api/v1/auth/signup",
                "/api/v1/products",
                "/api/v1/products/*",
                "/api/v1/search"
            ).permitAll()

            // 그 외의 다른 모든 요청은 인증을 거쳐야 함
            .anyRequest().authenticated()
        );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt의 경우 보안 성능이 강력하나 해시 연산 속도가 느림. 현재 서비스 로직 상 Transaction 내에서 암호화를 진행 해 병목 발생 가능성 높음.
    // TODO: service 클래스 분리 작업 필수
    return new BCryptPasswordEncoder();
  }

}
