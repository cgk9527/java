package com.ecommerce.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.entity.User;
import com.ecommerce.es.ProductDocument;
import com.ecommerce.es.ProductEsRepository;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SkuMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动初始化: 预热库存到 Redis, 商品索引到 ES, 初始化管理员账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final StockService stockService;
    private final ProductEsRepository productEsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        try {
            List<Sku> skus = skuMapper.selectList(null);
            skus.forEach(sku -> stockService.initStock(sku.getId()));
            log.info("库存已预热到 Redis, 数量={}", skus.size());
        } catch (Exception e) {
            log.warn("库存预热失败(请确认 Redis 与数据库可用): {}", e.getMessage());
        }

        try {
            List<Product> products = productMapper.selectList(
                    Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1));
            products.forEach(p -> productEsRepository.save(ProductDocument.from(p)));
            log.info("商品已索引到 ES, 数量={}", products.size());
        } catch (Exception e) {
            log.warn("商品索引失败(请确认 ES 可用): {}", e.getMessage());
        }
    }

    /**
     * 初始化默认管理员账号 admin / admin123(仅首次)
     */
    private void seedAdmin() {
        try {
            Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, "admin"));
            if (count == null || count == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setNickname("管理员");
                admin.setStatus(1);
                admin.setRole(1);
                LocalDateTime now = LocalDateTime.now();
                admin.setCreatedAt(now);
                admin.setUpdatedAt(now);
                userMapper.insert(admin);
                log.info("已初始化默认管理员账号 admin / admin123");
            }
        } catch (Exception e) {
            log.warn("初始化管理员账号失败: {}", e.getMessage());
        }
    }
}