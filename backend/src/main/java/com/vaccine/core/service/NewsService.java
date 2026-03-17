package com.vaccine.core.service;

import com.vaccine.dto.NewsRequest;
import com.vaccine.dto.NewsResponse;
import com.vaccine.core.model.News;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.NewsRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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

    public NewsResponse getNewsById(Long id) {
        News news = newsRepository.findById(id)
            .orElseThrow(() -> new AppException("News not found"));
        return toResponse(news);
    }

    @Transactional
    public NewsResponse createNews(NewsRequest request, String adminEmail) {
        News news = new News();
        news.setTitle(request.getTitle().trim());
        news.setContent(request.getContent().trim());
        news.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
        news.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        news.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        news.setActive(request.getActive() != null ? request.getActive() : true);
        news.setExpiresAt(request.getExpiresAt());
        news.setCreatedBy(userRepository.findByEmail(adminEmail).map(user -> user.getId()).orElse(null));
        news.setCategory(normalizeCategory(request.getCategory()));

        news = newsRepository.save(news);
        return toResponse(news);
    }

    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request) {
        News news = newsRepository.findById(id)
            .orElseThrow(() -> new AppException("News not found"));

        news.setTitle(request.getTitle().trim());
        news.setContent(request.getContent().trim());
        news.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
        news.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        if (request.getPriority() != null) news.setPriority(request.getPriority());
        if (request.getActive() != null) news.setActive(request.getActive());
        news.setExpiresAt(request.getExpiresAt());
        news.setCategory(normalizeCategory(request.getCategory()));

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
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setContent(news.getContent());
        response.setSummary(news.getSummary());
        response.setImageUrl(news.getImageUrl());
        response.setPriority(news.getPriority());
        response.setActive(news.getActive());
        response.setPublishedAt(news.getPublishedAt());
        response.setExpiresAt(news.getExpiresAt());
        response.setCategory(news.getCategory());
        return response;
    }

    public List<NewsResponse> getLatestPublishedNews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsRepository.findByPublishedTrueAndExpiresAtIsNullOrExpiresAtAfter(LocalDateTime.now(), pageable)
            .map(this::toResponse)
            .getContent();
    }
}
