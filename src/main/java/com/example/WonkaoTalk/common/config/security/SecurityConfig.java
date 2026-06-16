package com.example.WonkaoTalk.common.config.security;

import com.example.WonkaoTalk.common.config.properties.FrontendProperties;
import com.example.WonkaoTalk.common.config.security.jwt.JwtAuthenticationFilter;
import com.example.WonkaoTalk.common.config.security.jwt.JwtExceptionFilter;
import com.example.WonkaoTalk.domain.auth.service.OAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableConfigurationProperties(FrontendProperties.class)
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuth2UserService oAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final ClientRegistrationRepository clientRegistrationRepository;
  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    JwtExceptionFilter jwtExceptionFilter = new JwtExceptionFilter(objectMapper);
    http
        // REST API 서버이므로 CSRF 보호 비활성화
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // HTTP 요청에 대한 접근 권한 설정
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET,
                "/api/v1/products",
                "/api/v1/products/*",
                "/api/v1/products/categories"
            ).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("SELLER")
            .requestMatchers(
                "/api/v1/auth/check-email",
                "/api/v1/auth/login",
                "/api/v1/auth/reissue",
                "/api/v1/users/signup",
                "/api/v1/sellers/signup",
                "/api/v1/search",
                "/api/v1/search/reindex/**",
                "/ws/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll() // 인증 없이 접근 허용
            // 원활한 테스트를 위해 permitAll 설정
            .requestMatchers("/actuator/prometheus").permitAll()
            .requestMatchers(
                "/api/v1/auth/logout",
                "/api/v1/sellers/register"
            ).authenticated()
            .requestMatchers(
                "/api/v1/users/**",
                "/api/v1/friends/**"
            ).hasRole("USER")
            .requestMatchers(
                "/api/v1/sellers/**",
                "/api/v1/stores/**",
                "/api/v1/images/**"
            ).hasRole("SELLER")

            // SecurityTest용 엔드포인트
            .requestMatchers("/api/v1/health/public").permitAll()
            .requestMatchers("/api/v1/health/user").hasRole("USER")
            .requestMatchers("/api/v1/health/seller").hasRole("SELLER")
            .requestMatchers("/api/v1/health/admin").hasRole("ADMIN")

            .anyRequest().authenticated() // 그 외의 다른 모든 요청은 인증을 거쳐야 함
        )
        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(endpoint -> endpoint
                .authorizationRequestResolver(
                    customAuthorizationRequestResolver(clientRegistrationRepository)))
            .successHandler(oAuth2SuccessHandler)
            .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))

        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 프론트엔드 주소 명시적 허용
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

    // 허용할 HTTP 메서드
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    // 허용할 헤더
    configuration.setAllowedHeaders(
        Arrays.asList("Authorization", "Content-Type", "Cache-Control"));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_ADMIN > ROLE_USER_SELLER\n" +
            "ROLE_USER_SELLER > ROLE_USER\n" +
            "ROLE_USER_SELLER > ROLE_SELLER"
    );
  }

  private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {
    DefaultOAuth2AuthorizationRequestResolver defaultResolver =
        new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
            "/oauth2/authorization");

    return new OAuth2AuthorizationRequestResolver() {
      @Override
      public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request);
        return authRequest != null ? customizeAuthorizationRequest(authRequest) : null;
      }

      @Override
      public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
          String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request,
            clientRegistrationId);
        return authRequest != null ? customizeAuthorizationRequest(authRequest) : null;
      }
    };
  }

  private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest req) {
    Map<String, Object> extraParams = new HashMap<>(req.getAdditionalParameters());
    // 구글에 오프라인 접근(RT 발급) 요청
    if ("google".equals(req.getAttribute(OAuth2ParameterNames.REGISTRATION_ID))) {
      extraParams.put("access_type", "offline");
      extraParams.put("prompt", "consent");
    }
    return OAuth2AuthorizationRequest.from(req).additionalParameters(extraParams).build();
  }
}
