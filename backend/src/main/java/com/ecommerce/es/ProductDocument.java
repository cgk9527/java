package com.ecommerce.es;

import com.ecommerce.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * 商品 ES 索引文档
 * 说明: 中文分词建议安装 ik 分词插件, 将 name 字段 analyzer 改为 ik_max_word / ik_smart;
 * 为保证开箱即用, 这里使用默认 standard 分词, 并额外保留 nameKeyword 字段做通配检索。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "product")
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String nameKeyword;

    @Field(type = FieldType.Text)
    private String subtitle;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String mainImage;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Long)
    private Long sales;

    @Field(type = FieldType.Integer)
    private Integer status;

    public static ProductDocument from(Product product) {
        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .nameKeyword(product.getName())
                .subtitle(product.getSubtitle())
                .categoryId(product.getCategoryId())
                .mainImage(product.getMainImage())
                .price(product.getPrice())
                .sales(product.getSales())
                .status(product.getStatus())
                .build();
    }
}