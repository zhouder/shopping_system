package com.cst.campussecondhand.service;

import com.cst.campussecondhand.entity.Product;
import com.cst.campussecondhand.entity.User;
import com.cst.campussecondhand.repository.ProductRepository;
import com.cst.campussecondhand.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 搜索、筛选和排序
     */
    public List<Product> findProducts(String keyword, String category, String sortBy) {
        // 排序
        Sort sort = switch (sortBy) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "favorites" -> Sort.by(Sort.Direction.DESC, "favoriteCount"); // 新增：按收藏数降序
            default -> Sort.by(Sort.Direction.DESC, "createdTime"); // 默认按最新发布
        };

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 模糊查询
            if (StringUtils.hasText(keyword)) {
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
                Predicate descriptionLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + keyword.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(titleLike, descriptionLike));
            }

            // 品类筛选
            if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, sort);
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
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setLocation(productDetails.getLocation());

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

    // 新增：删除商品
    @Transactional
    public void deleteProduct(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("商品不存在, ID: " + productId);
        }
        productRepository.deleteById(productId);
    }
}

