package com.example.WonkaoTalk.domain.product.enums;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;

public enum ProductSortType {
  POPULAR, LATEST, PRICE_ASC, PRICE_DESC;

  public static ProductSortType from(String value) {
    try {
      return valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.PROD_INVALID_SORT);
    }
  }
}
