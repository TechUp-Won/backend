package com.example.WonkaoTalk.domain.search.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 단건 색인 서비스 단위 테스트. ProductDocument.from() 의 문서 변환 경로까지 함께 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductIndexServiceTest {

  @Mock
  private ProductRepo productRepo;

  @Mock
  private ProductOptionRepo productOptionRepo;

  @Mock
  private ProductSearchRepository searchRepository;

  @InjectMocks
  private ProductIndexService indexService;

  @Test
  @DisplayName("색인 대상이면 옵션값을 포함한 문서를 저장한다")
  void indexById_savesDocument_whenIndexable() {
    Product product = indexableProduct(1L);
    when(productRepo.findById(1L)).thenReturn(Optional.of(product));
    when(productOptionRepo.findOptionNamesByProductId(1L)).thenReturn(List.of("흰색", "검정"));

    indexService.indexById(1L);

    ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
    verify(searchRepository).save(captor.capture());
    ProductDocument doc = captor.getValue();
    assertThat(doc.getId()).isEqualTo(1L);
    assertThat(doc.getName()).isEqualTo("티셔츠");
    assertThat(doc.getOptionValues()).containsExactly("흰색", "검정");
    assertThat(doc.getCategoryId()).isEqualTo(10L);
    assertThat(doc.getStatus()).isEqualTo("ON_SALE");
    verify(searchRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("상품이 존재하지 않으면 색인 문서를 삭제한다")
  void indexById_deletesDocument_whenProductNotFound() {
    when(productRepo.findById(1L)).thenReturn(Optional.empty());

    indexService.indexById(1L);

    verify(searchRepository).deleteById(1L);
    verify(searchRepository, never()).save(any());
  }

  @Test
  @DisplayName("소프트 삭제된 상품이면 색인 문서를 삭제한다")
  void indexById_deletesDocument_whenSoftDeleted() {
    Product product = org.mockito.Mockito.mock(Product.class);
    when(product.getDeletedAt()).thenReturn(LocalDateTime.now());
    when(productRepo.findById(1L)).thenReturn(Optional.of(product));

    indexService.indexById(1L);

    verify(searchRepository).deleteById(1L);
    verify(searchRepository, never()).save(any());
  }

  @Test
  @DisplayName("판매상태가 노출 대상이 아니면 색인 문서를 삭제한다")
  void indexById_deletesDocument_whenStatusNotIndexable() {
    Product product = org.mockito.Mockito.mock(Product.class);
    when(product.getDeletedAt()).thenReturn(null);
    when(product.getStatus()).thenReturn(SaleStatus.STOP_SALE);
    when(productRepo.findById(1L)).thenReturn(Optional.of(product));

    indexService.indexById(1L);

    verify(searchRepository).deleteById(1L);
    verify(searchRepository, never()).save(any());
  }

  @Test
  @DisplayName("deleteById 는 색인 레포지토리에 위임한다")
  void deleteById_delegatesToRepository() {
    indexService.deleteById(9L);

    verify(searchRepository).deleteById(9L);
  }

  private Product indexableProduct(Long id) {
    Category category = org.mockito.Mockito.mock(Category.class);
    when(category.getId()).thenReturn(10L);

    Product product = org.mockito.Mockito.mock(Product.class);
    when(product.getId()).thenReturn(id);
    when(product.getName()).thenReturn("티셔츠");
    when(product.getDeletedAt()).thenReturn(null);
    when(product.getStatus()).thenReturn(SaleStatus.ON_SALE);
    when(product.getCategory()).thenReturn(category);
    when(product.getDiscountedPrice()).thenReturn(10000);
    when(product.getLikeCount()).thenReturn(5);
    when(product.getCreatedAt()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
    return product;
  }
}
