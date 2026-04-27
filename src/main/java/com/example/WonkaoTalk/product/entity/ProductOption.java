package com.example.WonkaoTalk.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_option")
public class ProductOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "product_option_id")
  private Long productOptionId;

  @Column(name = "option_name", nullable = false)
  private String optionName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_option_group_id", nullable = false)
  private ProductOptionGroup productOptionGroup;
}
