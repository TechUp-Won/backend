package com.example.WonkaoTalk.common.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class SecurityHealthCheckController {

  @GetMapping("/public")
  public ResponseEntity<ApiResponse<String>> publicHealth() {
    return ResponseEntity.ok(ApiResponse.success("공개 엔드포인트 접근 성공", "PUBLIC"));
  }

  @GetMapping("/user")
  public ResponseEntity<ApiResponse<String>> userHealth() {
    return ResponseEntity.ok(ApiResponse.success("USER 접근 성공", "USER"));
  }

  @GetMapping("/seller")
  public ResponseEntity<ApiResponse<String>> sellerHealth() {
    return ResponseEntity.ok(ApiResponse.success("SELLER 접근 성공", "SELLER"));
  }

  @GetMapping("/admin")
  public ResponseEntity<ApiResponse<String>> adminHealth() {
    return ResponseEntity.ok(ApiResponse.success("ADMIN 접근 성공", "ADMIN"));
  }

}
