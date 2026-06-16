package com.example.WonkaoTalk.domain.seller.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.seller.dto.SellerRegisterRequest;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SellerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AuthRepo authRepo;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private SellerRepo sellerRepo;

  @Autowired
  private StoreRepo storeRepo;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private Auth auth;
  private User user;
  private String testAccessToken;

  @BeforeEach
  void setUp() {
    auth = Auth.builder()
        .role(Role.USER)
        .build();
    authRepo.save(auth);

    user = User.builder()
        .nickname("닉네임")
        .name("이름")
        .phone("010-0000-0000")
        .gender(Gender.NONE)
        .image("default.png")
        .auth(auth)
        .build();
    userRepo.save(user);
  }

  @Test
  @DisplayName("정상적인 판매자 권한 승격 및 DB 데이터 적재 검증")
  @WithMockUser(username = "test@test.com", roles = "USER")
  void signUpAsSeller_Integration_Success() throws Exception {
    // given
    SellerRegisterRequest request = SellerRegisterRequest.builder()
        .buzNo("1234567890")
        .name("사업자")
        .phone("02-345-6789").build();
    testAccessToken = jwtTokenProvider.createAccessToken("test@test.com", auth.getId(),
        user.getId(), null, "USER");

    // when & then
    mockMvc.perform(post("/api/v1/sellers/register")
            .header("Authorization", "Bearer " + testAccessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()) // 자원의 '생성'이 아닌 기존 자원의 '수정/승격'이므로 200 OK 기대
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    Seller savedSeller = sellerRepo.findByAuth(auth).orElseThrow();
    assertThat(savedSeller.getBuzNo()).isEqualTo("1234567890");
    assertThat(savedSeller.getName()).isEqualTo("사업자");

    Auth updatedAuth = authRepo.findById(auth.getId()).orElseThrow();
    assertThat(updatedAuth.getRole()).isEqualTo(Role.USER_SELLER);
  }

  @Test
  @DisplayName("활성화된 스토어가 존재하는 경우 탈퇴 시도 시 차단 및 롤백 검증")
  @WithMockUser(username = "seller@test.com", roles = "SELLER")
  void withdrawSeller_Fail_WhenActiveStoreExists() throws Exception {
    // given
    auth.updateRole(Role.USER_SELLER);
    authRepo.save(auth);

    Seller seller = Seller.builder()
        .auth(auth)
        .buzNo("9999999999")
        .name("사업자")
        .phone("010-9999-9999")
        .build();
    sellerRepo.save(seller);

    Store store = Store.builder()
        .seller(seller)
        .name("내스토어")
        .phone("02-345-6789")
        .build();
    storeRepo.save(store);

    testAccessToken = jwtTokenProvider.createAccessToken("test@test.com", auth.getId(), null,
        seller.getId(), "SELLER");

    // when
    mockMvc.perform(delete("/api/v1/sellers/withdraw")
            .header("Authorization", "Bearer " + testAccessToken)
            .requestAttr("authId", auth.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("SELLER-HAS-STORE"));

    // then
    Seller targetSeller = sellerRepo.findById(seller.getId()).orElseThrow();
    assertThat(targetSeller.getDeletedAt()).isNull();
  }
}
