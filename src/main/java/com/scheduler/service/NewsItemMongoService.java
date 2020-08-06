package com.scheduler.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scheduler.model.NewsItem;
import com.scheduler.repository.NewsItemRepository;

@Service
public class NewsItemMongoService {

    @Autowired
    private NewsItemRepository newsItemRepository;

    public List<NewsItem> findAll() {

	List<NewsItem> items = newsItemRepository.findAll();

	return items;
    }

    public Long count() {

	return newsItemRepository.count();
    }

    public void deleteById(String userId) {

	newsItemRepository.deleteById(userId);
    }
}
