package com.example.WonkaoTalk.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "variant_option_map",
    uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "product_option_id"}))
public class VariantOptionMap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "variant_option_map_id")
  private Long variantOptionMapId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant productVariant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_option_id", nullable = false)
  private ProductOption productOption;
}