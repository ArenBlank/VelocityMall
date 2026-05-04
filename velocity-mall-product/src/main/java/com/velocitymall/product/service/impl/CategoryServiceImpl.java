package com.velocitymall.product.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitymall.common.exception.BusinessException;
import com.velocitymall.common.model.vo.CategoryTreeVO;
import com.velocitymall.common.result.ResultCode;
import com.velocitymall.product.entity.Category;
import com.velocitymall.product.mapper.CategoryMapper;
import com.velocitymall.product.service.CategoryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Product category service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_TREE_CACHE_KEY = "velocitymall:product:category:tree";

    private static final String CATEGORY_TREE_LOCK_KEY = "velocitymall:product:category:lock";

    private static final long LOCK_WAIT_SECONDS = 1L;

    private static final long LOCK_LEASE_SECONDS = 10L;

    private static final long EMPTY_CACHE_TTL_MINUTES = 5L;

    private static final long NORMAL_CACHE_BASE_DAYS = 7L;

    private static final long NORMAL_CACHE_RANDOM_HOURS = 24L;

    private static final Long ROOT_PARENT_ID = 0L;

    private final CategoryMapper categoryMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        List<CategoryTreeVO> cachedTree = getCachedCategoryTree();
        if (cachedTree != null) {
            return cachedTree;
        }

        RLock lock = redissonClient.getLock(CATEGORY_TREE_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ResultCode.BIZ_WARNING, "系统繁忙，请稍后重试");
            }

            cachedTree = getCachedCategoryTree();
            if (cachedTree != null) {
                return cachedTree;
            }

            List<CategoryTreeVO> categoryTree = buildCategoryTreeFromDatabase();
            cacheCategoryTree(categoryTree);
            return categoryTree;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查询分类树被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private List<CategoryTreeVO> getCachedCategoryTree() {
        String cachedJson = stringRedisTemplate.opsForValue().get(CATEGORY_TREE_CACHE_KEY);
        if (!StringUtils.hasText(cachedJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, new TypeReference<List<CategoryTreeVO>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("Category tree cache JSON parse failed, cache will be deleted. cacheKey: {}",
                    CATEGORY_TREE_CACHE_KEY, exception);
            stringRedisTemplate.delete(CATEGORY_TREE_CACHE_KEY);
            return null;
        }
    }

    private List<CategoryTreeVO> buildCategoryTreeFromDatabase() {
        List<Category> categories = categoryMapper.selectEnabledCategories();
        if (CollectionUtils.isEmpty(categories)) {
            return List.of();
        }

        Map<Long, CategoryTreeVO> nodeMap = new LinkedHashMap<>(categories.size());
        for (Category category : categories) {
            nodeMap.put(category.getId(), convertToNode(category));
        }

        List<CategoryTreeVO> roots = new ArrayList<>();
        for (Category category : categories) {
            CategoryTreeVO node = nodeMap.get(category.getId());
            Long parentId = category.getParentId();
            if (ROOT_PARENT_ID.equals(parentId)) {
                roots.add(node);
                continue;
            }

            CategoryTreeVO parent = nodeMap.get(parentId);
            if (parent == null) {
                log.warn("Category parent missing, skip orphan node. categoryId: {}, parentId: {}",
                        category.getId(), parentId);
                continue;
            }
            parent.getChildren().add(node);
        }
        return roots;
    }

    private CategoryTreeVO convertToNode(Category category) {
        return CategoryTreeVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .level(category.getLevel())
                .sort(category.getSort())
                .icon(category.getIcon())
                .children(new ArrayList<>())
                .build();
    }

    private void cacheCategoryTree(List<CategoryTreeVO> categoryTree) {
        try {
            String json = objectMapper.writeValueAsString(categoryTree);
            long ttlSeconds = CollectionUtils.isEmpty(categoryTree)
                    ? TimeUnit.MINUTES.toSeconds(EMPTY_CACHE_TTL_MINUTES)
                    : calculateNormalCacheTtlSeconds();
            stringRedisTemplate.opsForValue().set(CATEGORY_TREE_CACHE_KEY, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            log.error("Category tree JSON serialize failed", exception);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "分类树缓存写入失败");
        }
    }

    private long calculateNormalCacheTtlSeconds() {
        long randomHours = ThreadLocalRandom.current().nextLong(1, NORMAL_CACHE_RANDOM_HOURS + 1);
        return TimeUnit.DAYS.toSeconds(NORMAL_CACHE_BASE_DAYS) + TimeUnit.HOURS.toSeconds(randomHours);
    }
}
