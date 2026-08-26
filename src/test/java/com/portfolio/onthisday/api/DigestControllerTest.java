package com.portfolio.onthisday.api;

import com.portfolio.onthisday.api.dto.EventSummary;
import com.portfolio.onthisday.service.DigestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice test for {@link DigestController}: JSON shape and request validation.
 */
@WebMvcTest(DigestController.class)
class DigestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DigestService digestService;

    @Test
    void todayReturnsCuratedList() throws Exception {
        when(digestService.today()).thenReturn(List.of(
                new EventSummary(1L, 1969, "Apollo 11", "Apollo 11 lands on the Moon.",
                        null, "https://en.wikipedia.org/wiki/Apollo_11", "SELECTED", 9.5,
                        List.of("space", "1960s"))));

        mockMvc.perform(get("/api/digest/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].year").value(1969))
                .andExpect(jsonPath("$[0].tags[0]").value("space"));
    }

    @Test
    void invalidMonthIsRejected() throws Exception {
        mockMvc.perform(get("/api/digest/13/5"))
                .andExpect(status().isBadRequest());
    }
}
