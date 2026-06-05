package com.example.WonkaoTalk.domain.search.indexer;

import com.example.WonkaoTalk.domain.product.entity.Product;
import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.product.repo.ProductOptionRepo;
import com.example.WonkaoTalk.domain.product.repo.ProductRepo;
import com.example.WonkaoTalk.domain.search.document.ProductDocument;
import com.example.WonkaoTalk.domain.search.repo.ProductSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 문서 색인/삭제 로직. productId 기준으로 DB 에서 문서 전체를 다시 읽어 멱등하게 재색인한다.
 */
@Service
@RequiredArgsConstructor
public class ProductIndexService {

  /** 검색에 노출되는(=색인 대상) 판매상태. */
  private static final List<SaleStatus> INDEXABLE_STATUSES =
      List.of(SaleStatus.ON_SALE, SaleStatus.OUT_OF_STOCK);

  private final ProductRepo productRepo;
  private final ProductOptionRepo productOptionRepo;
  private final ProductSearchRepository searchRepository;

  /** productId 의 현재 상태를 색인에 반영. 색인 대상이 아니면 문서를 제거한다. */
  @Transactional(readOnly = true)
  public void indexById(Long productId) {
    Product product = productRepo.findById(productId).orElse(null);
    if (product == null || !isIndexable(product)) {
      searchRepository.deleteById(productId);
      return;
    }
    List<String> optionValues = productOptionRepo.findOptionNamesByProductId(productId);
    searchRepository.save(ProductDocument.from(product, optionValues));
  }

  public void deleteById(Long productId) {
    searchRepository.deleteById(productId);
  }

  private boolean isIndexable(Product product) {
    return product.getDeletedAt() == null && INDEXABLE_STATUSES.contains(product.getStatus());
  }
}
