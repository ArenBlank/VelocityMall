package com.velocitymall.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.review.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商品评价 Mapper。
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    @Update("UPDATE oms_product_review SET like_count = like_count + 1 WHERE id = #{reviewId} AND is_deleted = 0")
    int increaseLikeCount(@Param("reviewId") Long reviewId);

    @Update("UPDATE oms_product_review SET dislike_count = dislike_count + 1 WHERE id = #{reviewId} AND is_deleted = 0")
    int increaseDislikeCount(@Param("reviewId") Long reviewId);

    @Update("UPDATE oms_product_review SET reply_count = reply_count + 1 WHERE id = #{reviewId} AND is_deleted = 0")
    int increaseReplyCount(@Param("reviewId") Long reviewId);

    @Update("""
            UPDATE oms_product_review
            SET like_count = like_count - 1
            WHERE id = #{reviewId}
              AND like_count > 0
              AND is_deleted = 0
            """)
    int decreaseLikeCount(@Param("reviewId") Long reviewId);

    @Update("""
            UPDATE oms_product_review
            SET dislike_count = dislike_count - 1
            WHERE id = #{reviewId}
              AND dislike_count > 0
              AND is_deleted = 0
            """)
    int decreaseDislikeCount(@Param("reviewId") Long reviewId);

    @Update("""
            UPDATE oms_product_review
            SET reply_count = reply_count - 1
            WHERE id = #{reviewId}
              AND reply_count > 0
              AND is_deleted = 0
            """)
    int decreaseReplyCount(@Param("reviewId") Long reviewId);

    @Update("""
            UPDATE oms_product_review
            SET like_count = like_count - 1,
                dislike_count = dislike_count + 1
            WHERE id = #{reviewId}
              AND like_count > 0
              AND is_deleted = 0
            """)
    int switchLikeToDislike(@Param("reviewId") Long reviewId);

    @Update("""
            UPDATE oms_product_review
            SET like_count = like_count + 1,
                dislike_count = dislike_count - 1
            WHERE id = #{reviewId}
              AND dislike_count > 0
              AND is_deleted = 0
            """)
    int switchDislikeToLike(@Param("reviewId") Long reviewId);

    @Select("SELECT COUNT(1) FROM oms_product_review WHERE spu_id = #{spuId} AND is_deleted = 0")
    Long countBySpuId(@Param("spuId") Long spuId);

    @Select("""
            SELECT COUNT(1)
            FROM oms_product_review
            WHERE spu_id = #{spuId}
              AND rating >= 4
              AND is_deleted = 0
            """)
    Long countGoodBySpuId(@Param("spuId") Long spuId);
}
