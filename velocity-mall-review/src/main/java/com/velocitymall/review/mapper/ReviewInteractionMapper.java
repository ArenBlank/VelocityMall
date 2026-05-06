package com.velocitymall.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.velocitymall.review.entity.ReviewInteraction;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 评价互动 Mapper。
 */
@Mapper
public interface ReviewInteractionMapper extends BaseMapper<ReviewInteraction> {

    @Select("""
            SELECT id, review_id, user_id, interaction_type, create_time, update_time, is_deleted
            FROM oms_review_interaction
            WHERE review_id = #{reviewId}
              AND user_id = #{userId}
              AND is_deleted = 0
            LIMIT 1
            """)
    ReviewInteraction selectActiveByReviewAndUser(@Param("reviewId") Long reviewId, @Param("userId") Long userId);

    @Update("""
            UPDATE oms_review_interaction
            SET interaction_type = #{interactionType},
                update_time = NOW()
            WHERE id = #{id}
              AND is_deleted = 0
            """)
    int updateInteractionType(@Param("id") Long id, @Param("interactionType") Integer interactionType);

    @Delete("""
            DELETE FROM oms_review_interaction
            WHERE id = #{id}
              AND review_id = #{reviewId}
              AND user_id = #{userId}
            """)
    int physicalDelete(@Param("id") Long id, @Param("reviewId") Long reviewId, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT id, review_id, user_id, interaction_type, create_time, update_time, is_deleted
            FROM oms_review_interaction
            WHERE user_id = #{userId}
              AND is_deleted = 0
              AND review_id IN
              <foreach collection="reviewIds" item="reviewId" open="(" separator="," close=")">
                #{reviewId}
              </foreach>
            </script>
            """)
    List<ReviewInteraction> selectActiveByUserAndReviewIds(
            @Param("userId") Long userId,
            @Param("reviewIds") List<Long> reviewIds
    );
}
