package com.vaccine.core.service;

import com.vaccine.common.dto.NewsRequest;
import com.vaccine.common.dto.NewsResponse;
import com.vaccine.domain.News;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.NewsRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NewsService {
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;

    public NewsService(NewsRepository newsRepository, UserRepository userRepository) {
        this.newsRepository = newsRepository;
        this.userRepository = userRepository;
    }

    public Page<NewsResponse> getActiveNews(Pageable pageable) {
        return newsRepository.findActiveNews(LocalDateTime.now(), pageable)
            .map(this::toResponse);
    }

    public List<NewsResponse> getPublicNews() {
        return newsRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(news -> Boolean.TRUE.equals(news.getActive()))
            .filter(news -> news.getExpiresAt() == null || news.getExpiresAt().isAfter(LocalDateTime.now()))
            .map(this::toResponse)
            .toList();
    }

    public Page<NewsResponse> getAllNews(Pageable pageable) {
        return newsRepository.findAll(pageable).map(this::toResponse);
    }

    public NewsResponse getNewsById(Long id) {
        News news = newsRepository.findById(id)
            .orElseThrow(() -> new AppException("News not found"));
        return toResponse(news);
    }

    @Transactional
    public NewsResponse createNews(NewsRequest request, String adminEmail) {
        News news = News.builder()
            .title(request.title().trim())
            .content(request.content().trim())
            .summary(request.summary() != null ? request.summary().trim() : null)
            .imageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null)
            .priority(request.priority() != null ? request.priority() : 0)
            .active(request.active() != null ? request.active() : true)
            .expiresAt(request.expiresAt())
            .createdBy(userRepository.findByEmail(adminEmail).map(user -> user.getId()).orElse(null))
            .category(normalizeCategory(request.category()))
            .build();

        news = newsRepository.save(news);
        return toResponse(news);
    }

    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request) {
        News news = newsRepository.findById(id)
            .orElseThrow(() -> new AppException("News not found"));

        news.setTitle(request.title().trim());
        news.setContent(request.content().trim());
        news.setSummary(request.summary() != null ? request.summary().trim() : null);
        news.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);
        if (request.priority() != null) news.setPriority(request.priority());
        if (request.active() != null) news.setActive(request.active());
        news.setExpiresAt(request.expiresAt());
        news.setCategory(normalizeCategory(request.category()));

        news = newsRepository.save(news);
        return toResponse(news);
    }

    @Transactional
    public void deleteNews(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new AppException("News not found");
        }
        newsRepository.deleteById(id);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "GENERAL";
        }
        return category.trim().toUpperCase();
    }

    private NewsResponse toResponse(News news) {
        return new NewsResponse(
            news.getId(),
            news.getTitle(),
            news.getContent(),
            news.getSummary(),
            news.getImageUrl(),
            news.getPriority(),
            news.getActive(),
            news.getPublishedAt(),
            news.getExpiresAt(),
            news.getCategory(),
            news.getCreatedAt(),
            news.getUpdatedAt()
        );
    }

    public List<NewsResponse> getLatestPublishedNews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsRepository.findByPublishedTrueAndExpiresAtIsNullOrExpiresAtAfter(LocalDateTime.now(), pageable)
            .map(this::toResponse)
            .getContent();
    }
}
