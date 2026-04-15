package com.once.globalnews.news.domain;

import com.once.globalnews.global.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "news_sources")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NewsSource extends BaseEntity {

    @Id
    @Column(length = 255)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(length = 10)
    private String country;
}
