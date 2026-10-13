package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends Repository<Post, String> {
    interface Usage {
        String getName();
        long getPostsCount();
    }

    @Query("select t as name, count(distinct p.id) as postsCount from Post p join p.tags t " +
            "where p.status = 'PUBLISHED' " +
            "and lower(t) like lower(concat('%', :search, '%')) " +
            "group by t order by count(distinct p.id) desc, t asc")
    List<Usage> findPopular(@Param("search") String search, Pageable pageable);
}
