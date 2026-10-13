package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.PostImageFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface PostImageFeaturesRepository extends JpaRepository<PostImageFeatures, String> {
    List<PostImageFeatures> findByPostIdIn(Collection<String> postIds);
}
