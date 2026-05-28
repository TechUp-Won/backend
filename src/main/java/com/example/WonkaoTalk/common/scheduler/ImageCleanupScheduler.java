package com.example.WonkaoTalk.common.scheduler;

import com.example.WonkaoTalk.domain.image.service.ImageService;
import com.example.WonkaoTalk.domain.order.repo.OrderItemRepo;
import com.example.WonkaoTalk.domain.product.entity.DeletedProductImage;
import com.example.WonkaoTalk.domain.product.repo.DeletedProductImageRepo;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

  private static final int BATCH_SIZE = 1000;

  private final DeletedProductImageRepo deletedProductImageRepo;
  private final OrderItemRepo orderItemRepo;
  private final ImageService imageService;

  @Scheduled(cron = "0 0 2 * * *")
  public void cleanupDeletedProductImages() {
    List<DeletedProductImage> candidates = deletedProductImageRepo.findAll(PageRequest.of(0, BATCH_SIZE)).getContent();
    if (candidates.isEmpty()) {
      return;
    }

    List<String> urls = candidates.stream()
        .map(DeletedProductImage::getUrl)
        .toList();

    Set<String> referencedUrls = orderItemRepo.findReferencedImageUrls(urls);

    List<DeletedProductImage> toDelete = candidates.stream()
        .filter(img -> !referencedUrls.contains(img.getUrl()))
        .toList();

    List<DeletedProductImage> successfullyDeleted = toDelete.stream()
        .filter(img -> {
          try {
            imageService.deleteByUrl(img.getUrl());
            return true;
          } catch (Exception e) {
            log.error("S3 이미지 삭제 실패 (다음 실행 시 재시도): {}", img.getUrl(), e);
            return false;
          }
        })
        .toList();

    List<DeletedProductImage> referencedImages = candidates.stream()
        .filter(img -> referencedUrls.contains(img.getUrl()))
        .toList();

    deletedProductImageRepo.deleteAllInBatch(successfullyDeleted);
    deletedProductImageRepo.deleteAllInBatch(referencedImages);
    log.info("이미지 정리 완료 - S3 삭제: {}건, 주문 참조로 S3 보존: {}건",
        successfullyDeleted.size(), referencedImages.size());
  }
}