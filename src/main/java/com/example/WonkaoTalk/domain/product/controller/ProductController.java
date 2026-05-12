package com.example.WonkaoTalk.domain.product.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.product.dto.CategoryResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductListRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse;
import com.example.WonkaoTalk.domain.product.service.CategoryService;
import com.example.WonkaoTalk.domain.product.service.ProductCreateService;
import com.example.WonkaoTalk.domain.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;
  private final ProductCreateService productCreateService;
  private final CategoryService categoryService;

  @PostMapping
  public ResponseEntity<ApiResponse<ProductCreateResponse>> createProduct(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ProductCreateRequest request) {
    ProductCreateResponse response = productCreateService.create(userDetails.getAuthId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("상품이 등록되었습니다", response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
      @ModelAttribute ProductListRequest request) {
    ProductListResponse response = productService.getProductList(request);
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @GetMapping("/{productId}")
  public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
      @PathVariable Long productId) {
    ProductDetailResponse response = productService.getProductDetail(productId);
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @GetMapping("/categories")
  public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
    List<CategoryResponse> response = categoryService.getCategoryTree();
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }
}
