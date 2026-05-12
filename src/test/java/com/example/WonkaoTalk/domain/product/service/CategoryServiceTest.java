package com.example.WonkaoTalk.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.domain.product.dto.CategoryResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock
  private CategoryRepo categoryRepository;

  @InjectMocks
  private CategoryService categoryService;

  // ── 정상 조회 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("최상위 카테고리만 있으면 children이 빈 배열로 반환된다")
  void returnsRootWithEmptyChildren_whenNoChildExists() {
    Category root = mockCategory(1L, "의류", 0, null);
    when(categoryRepository.findAllWithParent()).thenReturn(List.of(root));

    List<CategoryResponse> result = categoryService.getCategoryTree();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(1L);
    assertThat(result.get(0).name()).isEqualTo("의류");
    assertThat(result.get(0).depth()).isEqualTo(0);
    assertThat(result.get(0).children()).isEmpty();
  }

  @Test
  @DisplayName("대분류 아래 소분류가 children 배열로 중첩된다")
  void nestChildren_underParentCategory() {
    Category parent = mockCategory(1L, "의류", 0, null);
    Category child1 = mockCategory(2L, "상의", 1, parent);
    Category child2 = mockCategory(3L, "하의", 1, parent);
    when(categoryRepository.findAllWithParent()).thenReturn(List.of(parent, child1, child2));

    List<CategoryResponse> result = categoryService.getCategoryTree();

    assertThat(result).hasSize(1);
    CategoryResponse clothing = result.get(0);
    assertThat(clothing.children()).hasSize(2);
    assertThat(clothing.children()).extracting(CategoryResponse::name)
        .containsExactlyInAnyOrder("상의", "하의");
  }

  @Test
  @DisplayName("소분류의 children은 항상 빈 배열이다")
  void childrenOfLeaf_isAlwaysEmpty() {
    Category parent = mockCategory(1L, "의류", 0, null);
    Category child = mockCategory(2L, "상의", 1, parent);
    when(categoryRepository.findAllWithParent()).thenReturn(List.of(parent, child));

    List<CategoryResponse> result = categoryService.getCategoryTree();

    CategoryResponse leaf = result.get(0).children().get(0);
    assertThat(leaf.children()).isEmpty();
  }

  @Test
  @DisplayName("대분류가 여러 개면 각각 독립된 트리로 반환된다")
  void returnsMultipleRoots_whenMultipleParentCategoriesExist() {
    Category clothing = mockCategory(1L, "의류", 0, null);
    Category books = mockCategory(2L, "도서", 0, null);
    Category top = mockCategory(3L, "상의", 1, clothing);
    Category dev = mockCategory(4L, "개발", 1, books);
    when(categoryRepository.findAllWithParent()).thenReturn(List.of(clothing, books, top, dev));

    List<CategoryResponse> result = categoryService.getCategoryTree();

    assertThat(result).hasSize(2);
    CategoryResponse clothingRes = result.stream()
        .filter(r -> r.id().equals(1L)).findFirst().orElseThrow();
    CategoryResponse booksRes = result.stream()
        .filter(r -> r.id().equals(2L)).findFirst().orElseThrow();

    assertThat(clothingRes.children()).hasSize(1);
    assertThat(clothingRes.children().get(0).name()).isEqualTo("상의");
    assertThat(booksRes.children()).hasSize(1);
    assertThat(booksRes.children().get(0).name()).isEqualTo("개발");
  }

  @Test
  @DisplayName("카테고리가 하나도 없으면 빈 리스트를 반환한다")
  void returnsEmptyList_whenNoCategoriesExist() {
    when(categoryRepository.findAllWithParent()).thenReturn(List.of());

    List<CategoryResponse> result = categoryService.getCategoryTree();

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("응답의 depth 값이 엔티티의 depth와 일치한다")
  void depthField_matchesEntityDepth() {
    Category parent = mockCategory(1L, "의류", 0, null);
    Category child = mockCategory(2L, "상의", 1, parent);
    when(categoryRepository.findAllWithParent()).thenReturn(List.of(parent, child));

    List<CategoryResponse> result = categoryService.getCategoryTree();

    assertThat(result.get(0).depth()).isEqualTo(0);
    assertThat(result.get(0).children().get(0).depth()).isEqualTo(1);
  }

  // ── 헬퍼 ────────────────────────────────────────────────────────────────────

  private Category mockCategory(Long id, String name, int depth, Category parent) {
    Category category = mock(Category.class);
    when(category.getId()).thenReturn(id);
    when(category.getName()).thenReturn(name);
    when(category.getDepth()).thenReturn(depth);
    when(category.getParentCategory()).thenReturn(parent);
    return category;
  }
}
