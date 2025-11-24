package com.cst.shopping_system.service;

import com.cst.shopping_system.entity.Product;
import com.cst.shopping_system.entity.User;
import com.cst.shopping_system.repository.ProductRepository;
import com.cst.shopping_system.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // 使用 System.getProperty("user.home") 获取当前用户的主目录
    public static final String UPLOAD_DIR = System.getProperty("user.home") + "/app-uploads/"; // 上传的图片存储地址


    public Product createProduct(Product product, Integer sellerId, MultipartFile[] files) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("卖家用户不存在, ID: " + sellerId));
        // 处理图片上传
        List<String> imageUrls = new ArrayList<>();
        if (files != null && files.length > 0) {
            File uploadDir = new File(UPLOAD_DIR);

            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                // 生成唯一文件名
                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                try {
                    Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR + uniqueFileName), StandardCopyOption.REPLACE_EXISTING);
                    imageUrls.add("/uploads/" + uniqueFileName);
                } catch (IOException e) {
                    throw new RuntimeException("文件上传失败: " + file.getOriginalFilename(), e);
                }
            }
        }

        product.setSeller(seller);
        product.setCreatedTime(new Date());
        // 将图片URL列表合并成一个字符串
        product.setImageUrls(String.join(",", imageUrls));

        return productRepository.save(product);
    }

    public List<Product> findAllProducts() {
        // 按创建时间倒序
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdTime"));
    }

    public Product findProductById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在, ID: " + id));
    }

    /**
     * 搜索、多选筛选、排序和分页
     */
    public Page<Product> findProducts(String keyword, List<String> categories, String sortBy, int page, int size) {
        // 排序
        Sort sort = switch (sortBy) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "sales" -> Sort.by(Sort.Direction.DESC, "sales");
            case "favorites" -> Sort.by(Sort.Direction.DESC, "favoriteCount");
            default -> Sort.by(Sort.Direction.DESC, "createdTime");
        };

        // 分页请求
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), 1));

            // 模糊查询
            if (StringUtils.hasText(keyword)) {
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
                Predicate descriptionLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + keyword.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(titleLike, descriptionLike));
            }

            // 多品类筛选 (使用 IN 语句)
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").in(categories));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 返回分页结果
        return productRepository.findAll(spec, pageable);
    }

    // 新增：根据卖家ID查找其所有商品
    public List<Product> findProductsBySellerId(Integer sellerId) {
        return productRepository.findBySellerIdOrderByCreatedTimeDesc(sellerId);
    }

    // 修改：更新商品信息的方法
    @Transactional
    public Product updateProduct(Integer productId, Product productDetails, List<String> existingImageUrls, MultipartFile[] newFiles) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在, ID: " + productId));

        existingProduct.setTitle(productDetails.getTitle());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setStock(productDetails.getStock());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setCategory(productDetails.getCategory());


        // 处理新上传的图片
        List<String> newImageUrls = new ArrayList<>();
        if (newFiles != null && newFiles.length > 0) {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            for (MultipartFile file : newFiles) {
                if (file.isEmpty()) continue;
                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                try {
                    Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR + uniqueFileName), StandardCopyOption.REPLACE_EXISTING);
                    newImageUrls.add("/uploads/" + uniqueFileName);
                } catch (IOException e) {
                    throw new RuntimeException("文件上传失败: " + file.getOriginalFilename(), e);
                }
            }
        }

        // 合并旧图片和新图片
        List<String> finalImageUrls = Stream.concat(
                existingImageUrls == null ? Stream.empty() : existingImageUrls.stream(),
                newImageUrls.stream()
        ).collect(Collectors.toList());

        existingProduct.setImageUrls(String.join(",", finalImageUrls));

        return productRepository.save(existingProduct);
    }

    // 修改后：逻辑下架（只是把 status 改为 0）
    public void deleteProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在, ID: " + productId));

        product.setStatus(0); // 0 表示下架
        productRepository.save(product); // 保存更改
    }

    // ★★★ 新增方法 ★★★
    @Transactional
    public void updateProductStatus(Integer productId, Integer status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        product.setStatus(status);
        productRepository.save(product);
    }
}

