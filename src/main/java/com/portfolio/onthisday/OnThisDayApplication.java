package com.portfolio.onthisday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the "On This Day" application.
 *
 * <p>The app fetches real historical events from the Wikipedia "On This Day" API,
 * persists and curates them, and serves them through a Giphy-style UI.
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class OnThisDayApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnThisDayApplication.class, args);
    }
}
