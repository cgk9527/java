package com.ecommerce.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;

    private Long userId;

    private String nickname;

    private Integer rating;

    private String content;

    private LocalDateTime createdAt;
}