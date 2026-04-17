package com.once.globalnews.news.infrastructure.persistence;

import com.once.globalnews.news.domain.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    Optional<NewsArticle> findByArticleId(String articleId);

    @Query("SELECT na FROM NewsArticle na " +
           "WHERE (COALESCE(:countries, NULL) IS NULL OR na.country IN :countries) " +
           "AND (:category IS NULL OR na.category = :category) " +
           "AND na.fullContentS3Key IS NOT NULL " +
           "ORDER BY na.publishedAt DESC")
    Page<NewsArticle> findByCountriesAndCategory(
            @Param("countries") List<String> countries,
            @Param("category") String category,
            Pageable pageable);
}
