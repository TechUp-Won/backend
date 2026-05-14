package com.example.WonkaoTalk.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.dto.StoreCreateRequest;
import com.example.WonkaoTalk.domain.store.dto.StoreResponse;
import com.example.WonkaoTalk.domain.store.dto.StoreUpdateRequest;
import com.example.WonkaoTalk.domain.store.entity.Store;
import com.example.WonkaoTalk.domain.store.repo.StoreRepo;
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
class StoreServiceTest {

  @InjectMocks
  private StoreService storeService;

  @Mock
  private StoreRepo storeRepo;

  private Seller seller;
  private Store store;

  @BeforeEach
  void setUp() {
    Auth auth = Auth.builder().build();
    seller = Seller.builder()
        .buzNo("1234567890")
        .name("사업자")
        .phone("02-3456-7890")
        .auth(auth)
        .build();
    ReflectionTestUtils.setField(seller, "id", 1L);

    store = Store.builder()
        .name("스토어1")
        .description("설명")
        .phone("02-1111-1111")
        .thumbnail("기본 썸네일")
        .seller(seller)
        .build();
    ReflectionTestUtils.setField(store, "id", 1L);
  }

  @Test
  @DisplayName("스토어 생성 성공")
  public void createStoreSuccess() {
    //given
    StoreCreateRequest request = new StoreCreateRequest("스토어", "설명", "02-3456-7890", "썸네일");

    given(storeRepo.findBySeller(seller)).willReturn(Optional.empty());
    given(storeRepo.existsByName(request.name())).willReturn(false);

    Store savedStore = Store.builder()
        .name(request.name())
        .description(request.description())
        .phone(request.phone())
        .thumbnail(request.thumbnail())
        .seller(seller)
        .build();
    ReflectionTestUtils.setField(savedStore, "id", 2L);
    given(storeRepo.save(any(Store.class))).willReturn(savedStore);

    //when
    StoreResponse response = storeService.createStore(seller, request);

    //then
    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("스토어");
    assertThat(response.sellerId()).isEqualTo(seller.getId());
    verify(storeRepo).save(any(Store.class));
  }

  @Test
  @DisplayName("스토어 조회 성공")
  public void getStoreSuccess() {
    // given
    given(storeRepo.findBySeller(seller)).willReturn(Optional.of(store));

    // when
    StoreResponse response = storeService.getStore(seller);

    // then
    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("스토어1");
    assertThat(response.id()).isEqualTo(1L);
  }

  @Test
  @DisplayName("스토어 수정 성공")
  public void updateStoreSuccess() {
    // given
    StoreUpdateRequest request = new StoreUpdateRequest("새 스토어", "새 설명", null, null);
    given(storeRepo.findBySeller(seller)).willReturn(Optional.of(store));

    // when
    StoreResponse response = storeService.updateStore(seller, request);

    // then
    assertThat(response.name()).isEqualTo("새 스토어");
    assertThat(response.description()).isEqualTo("새 설명");
    assertThat(response.phone()).isEqualTo("02-1111-1111");
    assertThat(response.thumbnail()).isEqualTo("기본 썸네일");
  }

  @Test
  @DisplayName("스토어 삭제(Soft Delete) 성공")
  public void deleteStoreSuccess() {
    // given
    given(storeRepo.findBySeller(seller)).willReturn(Optional.of(store));

    // when
    storeService.deleteStore(seller);

    // then
    assertThat(store.getName()).contains("삭제된 스토어");
    assertThat(store.getDescription()).isEqualTo("DELETED");
    assertThat(store.getPhone()).isEqualTo("000-0000-0000");
  }
}