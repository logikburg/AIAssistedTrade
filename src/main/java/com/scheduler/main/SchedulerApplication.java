package com.scheduler.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.scheduler.repository.NewsItemRepository;

@ComponentScan({"com.scheduler.*"})
@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = NewsItemRepository.class)
public class SchedulerApplication {

    public static void main(String[] args) {
	SpringApplication.run(SchedulerApplication.class, args);
    }

}
