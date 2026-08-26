package com.portfolio.onthisday.web;

import com.portfolio.onthisday.api.dto.EventDetail;
import com.portfolio.onthisday.api.dto.PageResponse;
import com.portfolio.onthisday.api.dto.RelatedResponse;
import com.portfolio.onthisday.service.DigestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Server-rendered Thymeleaf pages. The chrome (header, date, tag chips) and the first page
 * of tiles are rendered here; the client-side script then appends further pages from the
 * JSON API for infinite scroll. The detail page is fully server-rendered (a real page, not
 * a modal), per the Giphy-style navigation.
 */
@Controller
public class ViewController {

    private static final int FIRST_PAGE_SIZE = 24;

    private final DigestService digestService;

    public ViewController(DigestService digestService) {
        this.digestService = digestService;
    }

    /** Home = today's date. */
    @GetMapping("/")
    public String home(@RequestParam(required = false) String tag, Model model) {
        LocalDate today = LocalDate.now();
        return renderDay(today.getMonthValue(), today.getDayOfMonth(), tag, model);
    }

    /** Browse any calendar day. */
    @GetMapping("/date/{month}/{day}")
    public String day(@PathVariable int month,
                      @PathVariable int day,
                      @RequestParam(required = false) String tag,
                      Model model) {
        validateDate(month, day);
        return renderDay(month, day, tag, model);
    }

    @GetMapping("/event/{id}")
    public String event(@PathVariable Long id, Model model) {
        EventDetail detail = digestService.getEvent(id);
        RelatedResponse related = digestService.getRelated(id);

        model.addAttribute("event", detail);
        model.addAttribute("related", related);
        model.addAttribute("dateLabel", dateLabel(detail.month(), detail.day()));
        return "detail";
    }

    private String renderDay(int month, int day, String tag, Model model) {
        String activeTag = (tag == null || tag.isBlank()) ? null : tag;
        PageResponse<?> firstPage = digestService.browse(month, day, activeTag, 0, FIRST_PAGE_SIZE);

        model.addAttribute("month", month);
        model.addAttribute("day", day);
        model.addAttribute("dateLabel", dateLabel(month, day));
        model.addAttribute("tags", digestService.tagsForDay(month, day));
        model.addAttribute("activeTag", activeTag);
        model.addAttribute("firstPage", firstPage);
        model.addAttribute("pageSize", FIRST_PAGE_SIZE);

        // Prev/next day navigation. Use a leap year so Feb 29 is reachable.
        LocalDate anchor = LocalDate.of(2020, month, day);
        LocalDate prev = anchor.minusDays(1);
        LocalDate next = anchor.plusDays(1);
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("prevDay", prev.getDayOfMonth());
        model.addAttribute("nextMonth", next.getMonthValue());
        model.addAttribute("nextDay", next.getDayOfMonth());
        return "home";
    }

    private String dateLabel(int month, int day) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return monthName + " " + day;
    }

    private void validateDate(int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid date: " + month + "/" + day);
        }
    }
}
