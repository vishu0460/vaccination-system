package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.NewsRequest;
import com.vaccine.common.dto.NewsResponse;
import com.vaccine.core.service.NewsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequestMapping({"/api/v1/news", "/api/news"})
public class NewsController {
    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NewsResponse>>> getActiveNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching active news page={} size={}", page, size);
        Page<NewsResponse> newsPage = newsService.getActiveNews(PageRequest.of(page, size, 
            Sort.by(Sort.Direction.DESC, "priority").and(Sort.by(Sort.Direction.DESC, "publishedAt"))));
        return ResponseEntity.ok(ApiResponse.success(newsPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsResponse>> getNews(@PathVariable Long id) {
        log.info("Fetching news ID={}", id);
        NewsResponse news = newsService.getNewsById(id);
        return ResponseEntity.ok(ApiResponse.success(news));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<NewsResponse>> createNews(@Valid @RequestBody NewsRequest request,
                                                    Authentication authentication) {
        log.info("Creating news by user={}, title={}", authentication.getName(), request.title());
        NewsResponse news = newsService.createNews(request, authentication.getName());
        return ResponseEntity.status(201).body(ApiResponse.success(news, "News created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<NewsResponse>> updateNews(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        log.info("Updating news ID={}, title={}", id, request.title());
        NewsResponse news = newsService.updateNews(id, request);
        return ResponseEntity.ok(ApiResponse.success(news, "News updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable Long id) {
        log.info("Deleting news ID={}", id);
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
