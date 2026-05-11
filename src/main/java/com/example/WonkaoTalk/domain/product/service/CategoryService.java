package com.example.WonkaoTalk.domain.product.service;

import com.example.WonkaoTalk.domain.product.dto.CategoryResponse;
import com.example.WonkaoTalk.domain.product.entity.Category;
import com.example.WonkaoTalk.domain.product.repo.CategoryRepo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepo categoryRepository;

  public List<CategoryResponse> getCategoryTree() {
    List<Category> all = categoryRepository.findAllWithParent();

    Map<Long, List<Category>> childrenByParentId = all.stream()
        .filter(c -> c.getParentCategory() != null)
        .collect(Collectors.groupingBy(c -> c.getParentCategory().getId()));

    return all.stream()
        .filter(c -> c.getParentCategory() == null)
        .map(root -> toResponse(root, childrenByParentId))
        .toList();
  }

  private CategoryResponse toResponse(Category category, Map<Long, List<Category>> childrenByParentId) {
    List<CategoryResponse> children = childrenByParentId
        .getOrDefault(category.getId(), List.of())
        .stream()
        .map(child -> new CategoryResponse(child.getId(), child.getName(), child.getDepth(), List.of()))
        .toList();

    return new CategoryResponse(category.getId(), category.getName(), category.getDepth(), children);
  }
}
