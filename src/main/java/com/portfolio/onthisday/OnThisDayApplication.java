package com.portfolio.onthisday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the "On This Day" application.
 *
 * <p>The app fetches real historical events from the Wikipedia "On This Day" API and
 * (in later build steps) persists, curates, and serves them through a Giphy-style UI.
 */
@SpringBootApplication
public class OnThisDayApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnThisDayApplication.class, args);
    }
}
