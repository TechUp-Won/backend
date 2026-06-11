package com.example.WonkaoTalk.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
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
  @DisplayName("스토어 생성 성공 - 썸네일이 null인 경우 기본 썸네일 적용")
  public void createStoreSuccessDefaultThumbnail() {
    // given
    StoreCreateRequest request = new StoreCreateRequest("스토어", "설명", "02-3456-7890", null);

    given(storeRepo.findBySeller(seller)).willReturn(Optional.empty());
    given(storeRepo.existsByName(request.name())).willReturn(false);

    Store savedStore = Store.builder()
        .name(request.name())
        .description(request.description())
        .phone(request.phone())
        .thumbnail("http://defaultThumbnail.png")
        .seller(seller)
        .build();
    ReflectionTestUtils.setField(savedStore, "id", 2L);
    given(storeRepo.save(any(Store.class))).willReturn(savedStore);

    // when
    StoreResponse response = storeService.createStore(seller, request);

    // then
    assertThat(response.thumbnail()).isEqualTo("http://defaultThumbnail.png");
  }

  @Test
  @DisplayName("스토어 생성 실패 - 이미 해당 판매자의 스토어가 존재하는 경우")
  public void createStoreFailAlreadyExists() {
    // given
    StoreCreateRequest request = new StoreCreateRequest("새 스토어", "설명", "02-3456-7890", "썸네일");
    given(storeRepo.findBySeller(seller)).willReturn(Optional.of(store));

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> storeService.createStore(seller, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_EXISTS_ALREADY);
  }

  @Test
  @DisplayName("스토어 생성 실패 - 이미 존재하는 스토어 이름인 경우")
  public void createStoreFailDuplicatedName() {
    // given
    StoreCreateRequest request = new StoreCreateRequest("스토어1", "설명", "02-3456-7890", "썸네일");
    given(storeRepo.findBySeller(seller)).willReturn(Optional.empty());
    given(storeRepo.existsByName(request.name())).willReturn(true);

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> storeService.createStore(seller, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_EXISTS_NAME);
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
  @DisplayName("스토어 조회 실패 - 스토어가 존재하지 않는 경우")
  public void getStoreFailNotFound() {
    // given
    given(storeRepo.findBySeller(seller)).willReturn(Optional.empty());

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> storeService.getStore(seller))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_NOT_FOUND);
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
  @DisplayName("스토어 수정 성공 - 일부 필드만 업데이트 시 기존 필드 유지")
  public void updateStoreSuccessPartialUpdate() {
    // given
    StoreUpdateRequest request = new StoreUpdateRequest(null, "", "02-2222-2222", "새 썸네일");
    given(storeRepo.findBySeller(seller)).willReturn(Optional.of(store));

    given(storeRepo.existsByName(request.name())).willReturn(false);

    // when
    StoreResponse response = storeService.updateStore(seller, request);

    // then
    assertThat(response.name()).isEqualTo("스토어1");
    assertThat(response.description()).isEqualTo("설명");
    assertThat(response.phone()).isEqualTo("02-2222-2222");
    assertThat(response.thumbnail()).isEqualTo("새 썸네일");
  }

  @Test
  @DisplayName("스토어 수정 실패 - 스토어가 존재하지 않는 경우")
  public void updateStoreFailNotFound() {
    // given
    StoreUpdateRequest request = new StoreUpdateRequest("새 스토어", "새 설명", null, null);
    given(storeRepo.findBySeller(seller)).willReturn(Optional.empty());

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> storeService.updateStore(seller, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_NOT_FOUND);
  }

  @Test
  @DisplayName("스토어 수정 실패 - 변경하려는 이름이 이미 존재하는 경우")
  public void updateStore_Fail_DuplicatedName() {
    // given
    StoreUpdateRequest request = new StoreUpdateRequest("이미 존재하는 이름", "새 설명", null, null);
    given(storeRepo.findBySeller(seller)).willReturn(Optional.of(store));
    given(storeRepo.existsByName(request.name())).willReturn(true);

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> storeService.updateStore(seller, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_EXISTS_NAME);
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

  @Test
  @DisplayName("스토어 삭제 실패 - 스토어가 존재하지 않는 경우")
  public void deleteStore_Fail_NotFound() {
    // given
    given(storeRepo.findBySeller(seller)).willReturn(Optional.empty());

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> storeService.deleteStore(seller))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_NOT_FOUND);
  }
}