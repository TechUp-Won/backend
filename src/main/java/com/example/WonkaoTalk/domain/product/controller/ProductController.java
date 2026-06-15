package com.example.WonkaoTalk.domain.product.controller;

import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.product.dto.CategoryResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductCreateResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductDetailResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductEditFormResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductLikeToggleResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductListRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductListResponse;
import com.example.WonkaoTalk.domain.product.dto.ProductUpdateRequest;
import com.example.WonkaoTalk.domain.product.dto.ProductUpdateResponse;
import com.example.WonkaoTalk.domain.product.dto.StockAdjustRequest;
import com.example.WonkaoTalk.domain.product.dto.StockAdjustResponse;
import com.example.WonkaoTalk.domain.product.service.CategoryService;
import com.example.WonkaoTalk.domain.product.service.ProductCreateService;
import com.example.WonkaoTalk.domain.product.service.ProductDeleteService;
import com.example.WonkaoTalk.domain.product.service.ProductLikeService;
import com.example.WonkaoTalk.domain.product.service.ProductService;
import com.example.WonkaoTalk.domain.product.service.ProductUpdateService;
import com.example.WonkaoTalk.domain.product.service.StockAdjustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "상품", description = "상품 등록, 상품 목록/상세 조회, 카테고리 조회 API")
public class ProductController {

  private final ProductService productService;
  private final ProductCreateService productCreateService;
  private final ProductUpdateService productUpdateService;
  private final ProductDeleteService productDeleteService;
  private final StockAdjustService stockAdjustService;
  private final CategoryService categoryService;
  private final ProductLikeService productLikeService;

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "상품 등록", description = "판매자가 상품 기본 정보, 상세 정보, 옵션, 이미지를 등록합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<ProductCreateResponse>> createProduct(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ProductCreateRequest request) {
    ProductCreateResponse response = productCreateService.create(userDetails.getAuthId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("상품이 등록되었습니다", response));
  }

  @Operation(summary = "상품 목록 조회", description = "카테고리, 정렬 조건 등을 기준으로 상품 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
      @ModelAttribute ProductListRequest request) {
    ProductListResponse response = productService.getProductList(request);
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보와 옵션 정보를 조회합니다.")
  @GetMapping("/{productId}")
  public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
      @PathVariable Long productId) {
    ProductDetailResponse response = productService.getProductDetail(productId);
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "상품 수정 폼 조회", description = "판매자가 상품 수정 폼을 구성하는 데 필요한 기존 정보를 조회합니다.")
  @GetMapping("/{productId}/edit")
  public ResponseEntity<ApiResponse<ProductEditFormResponse>> getProductEditForm(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long productId) {
    ProductEditFormResponse response = productUpdateService.getEditForm(
        userDetails.getAuthId(), productId);
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "상품 정보 수정", description = "판매자가 자신의 상품 기본 정보, 상세 설명, 이미지를 수정합니다.")
  @PatchMapping("/{productId}")
  public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long productId,
      @Valid @RequestBody ProductUpdateRequest request) {
    ProductUpdateResponse response = productUpdateService.update(
        userDetails.getAuthId(), productId, request);
    return ResponseEntity.ok(ApiResponse.success("상품 정보가 수정되었습니다", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "상품 삭제", description = "판매자가 자신의 상품을 소프트 삭제합니다. 진행 중인 주문이 있으면 삭제할 수 없습니다.")
  @DeleteMapping("/{productId}")
  public ResponseEntity<ApiResponse<Void>> deleteProduct(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long productId) {
    productDeleteService.delete(userDetails.getAuthId(), productId);
    return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다", null));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "재고 조정", description = "판매자가 특정 상품 옵션(variant)의 재고를 조정합니다.")
  @PatchMapping("/{productId}/variants/{variantId}/stock")
  public ResponseEntity<ApiResponse<StockAdjustResponse>> adjustStock(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long productId,
      @PathVariable Long variantId,
      @Valid @RequestBody StockAdjustRequest request) {
    StockAdjustResponse response = stockAdjustService.adjust(
        userDetails.getAuthId(), productId, variantId, request);
    return ResponseEntity.ok(ApiResponse.success("재고가 조정되었습니다", response));
  }

  @Operation(summary = "카테고리 조회", description = "상품 등록과 검색에 사용할 카테고리 트리를 조회합니다.")
  @GetMapping("/categories")
  public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
    List<CategoryResponse> response = categoryService.getCategoryTree();
    return ResponseEntity.ok(ApiResponse.success("조회가 완료되었습니다", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "상품 좋아요 토글", description = "특정 상품에 대한 좋아요를 추가하거나 취소합니다.")
  @PostMapping("/{productId}/likes")
  public ResponseEntity<ApiResponse<ProductLikeToggleResponse>> toggleLike(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long productId) {
    ProductLikeToggleResponse response = productLikeService.toggle(userDetails.getUserId(), productId);
    String message = response.isLiked() ? "상품 좋아요가 추가되었습니다." : "상품 좋아요가 취소되었습니다.";
    return ResponseEntity.ok(ApiResponse.success(message, response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "좋아요한 상품 목록 조회", description = "로그인한 사용자가 좋아요를 누른 상품 목록을 페이징하여 조회합니다.")
  @GetMapping("/likes")
  public ResponseEntity<ApiResponse<ProductLikeListResponse>> getLikedProducts(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    ProductLikeListResponse response = productLikeService.getLikedProducts(userDetails.getUserId(), pageable);
    return ResponseEntity.ok(ApiResponse.success("좋아요 상품 목록 조회 성공", response));
  }
}
