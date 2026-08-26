package com.portfolio.onthisday.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the DTOs map a representative Wikipedia "On This Day" payload, including the
 * nested pages/thumbnail/content_urls structure and unknown fields we intentionally ignore.
 */
class OnThisDayResponseMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsEventsWithNestedPages() throws Exception {
        String json = """
                {
                  "events": [
                    {
                      "text": "Apollo 11 lands the first humans on the Moon.",
                      "year": 1969,
                      "some_unknown_field": "should be ignored",
                      "pages": [
                        {
                          "title": "Apollo_11",
                          "extract": "Apollo 11 was the spaceflight that first landed humans on the Moon.",
                          "thumbnail": { "source": "https://example.org/thumb.jpg", "width": 320, "height": 213 },
                          "originalimage": { "source": "https://example.org/full.jpg", "width": 1600, "height": 1064 },
                          "content_urls": {
                            "desktop": { "page": "https://en.wikipedia.org/wiki/Apollo_11" },
                            "mobile": { "page": "https://en.m.wikipedia.org/wiki/Apollo_11" }
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        OnThisDayResponse response = objectMapper.readValue(json, OnThisDayResponse.class);

        assertThat(response.events()).hasSize(1);
        OnThisDayEvent event = response.events().get(0);
        assertThat(event.year()).isEqualTo(1969);
        assertThat(event.text()).contains("Apollo 11");

        assertThat(event.pages()).hasSize(1);
        WikiPage page = event.pages().get(0);
        assertThat(page.title()).isEqualTo("Apollo_11");
        assertThat(page.thumbnail().source()).isEqualTo("https://example.org/thumb.jpg");
        assertThat(page.content_urls().desktop().page())
                .isEqualTo("https://en.wikipedia.org/wiki/Apollo_11");
    }
}
