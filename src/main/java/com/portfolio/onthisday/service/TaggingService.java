package com.portfolio.onthisday.service;

import com.portfolio.onthisday.domain.Event;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Assigns lightweight category tags to an {@link Event} so the UI can offer "browse by
 * theme" chips and the API can answer "related events" queries.
 *
 * <p>Tagging is deliberately simple and deterministic: case-insensitive keyword matching
 * over the event text, title, and extract, plus tags derived from the feed type and the
 * year (a decade bucket). This needs no ML and no manual data entry — it runs on whatever
 * the live Wikipedia API returns.
 */
@Service
public class TaggingService {

    /** Ordered so the most specific/interesting themes are considered first. */
    private static final Map<String, List<String>> THEME_KEYWORDS = new LinkedHashMap<>();

    static {
        THEME_KEYWORDS.put("science", List.of(
                "scien", "physic", "chemist", "biolog", "astronom", "discover", "experiment",
                "telescope", "genome", "dna", "atom", "vaccine", "medicine", "nobel prize",
                "mathematic", "particle", "evolution", "theory of"));
        THEME_KEYWORDS.put("space", List.of(
                "space", "moon", "orbit", "astronaut", "cosmonaut", "satellite", "rocket",
                "nasa", "apollo", "spacecraft", "lunar", "mars", "planet", "comet"));
        THEME_KEYWORDS.put("technology", List.of(
                "invent", "patent", "computer", "internet", "telephone", "engine", "machine",
                "radio", "television", "aircraft", "airplane", "railway", "locomotive",
                "electric", "telegraph", "software", "transistor"));
        THEME_KEYWORDS.put("war", List.of(
                "war", "battle", "invasion", "invade", "army", "military", "troops", "siege",
                "revolt", "bombing", "bombard", "attack", "conflict", "surrender", "ceasefire",
                "naval", "soldier", "conquer"));
        THEME_KEYWORDS.put("politics", List.of(
                "president", "election", "elected", "government", "parliament", "king ", "queen ",
                "emperor", "empire", "independence", "constitution", "republic", "coup",
                "minister", "senate", "monarch", "inaugurat", "referendum", "treaty"));
        THEME_KEYWORDS.put("disaster", List.of(
                "earthquake", "flood", "hurricane", "wildfire", "disaster", "sinks", "sinking",
                "crash", "explosion", "eruption", "volcano", "famine", "plague", "epidemic",
                "pandemic", "tsunami", "tornado", "derail", "collapse"));
        THEME_KEYWORDS.put("exploration", List.of(
                "expedition", "explorer", "voyage", "summit of", "reaches the", "north pole",
                "south pole", "circumnavig", "discovers the", "first to reach", "colony", "settlers"));
        THEME_KEYWORDS.put("sports", List.of(
                "olympic", "championship", "world cup", "medal", "world record", "tournament",
                "grand prix", "athlete", "football", "baseball", "cricket", "marathon"));
        THEME_KEYWORDS.put("arts", List.of(
                "film", "movie", "novel", "painting", "symphony", "opera", "album", "premiere",
                "published", "artist", "musician", "band ", "poet", "sculpture", "theatre",
                "theater", "broadway", "exhibition"));
        THEME_KEYWORDS.put("religion", List.of(
                "church", "pope", "saint", "religious", "cathedral", "temple", "prophet",
                "bishop", "monk", "catholic", "islam", "christian", "buddhis", "canoniz"));
    }

    /**
     * Compute the set of tags for an event. The event must already have its text, feed
     * type, and year populated (i.e. after {@link EventMapper}).
     */
    public Set<String> tagsFor(Event event) {
        // TreeSet keeps tags stable/sorted, which makes output and tests deterministic.
        Set<String> tags = new TreeSet<>();

        String haystack = buildHaystack(event);
        Set<String> words = tokenize(haystack);

        for (Map.Entry<String, List<String>> theme : THEME_KEYWORDS.entrySet()) {
            for (String keyword : theme.getValue()) {
                if (matches(keyword, haystack, words)) {
                    tags.add(theme.getKey());
                    break;
                }
            }
        }

        // "Firsts" is a popular browsing theme in its own right.
        if (words.contains("first")) {
            tags.add("firsts");
        }

        // Feed-type tags (births/deaths/holidays) come straight from the source feed.
        switch (event.getFeedType()) {
            case BIRTHS -> tags.add("births");
            case DEATHS -> tags.add("deaths");
            case HOLIDAYS -> tags.add("holidays");
            default -> { /* events / selected carry only derived themes */ }
        }

        // Decade bucket, e.g. 1969 -> "1960s". Only for CE years we can format sensibly.
        Integer year = event.getYear();
        if (year != null && year > 0) {
            int decade = (year / 10) * 10;
            tags.add(decade + "s");
        }

        return tags;
    }

    /**
     * A keyword matches when: (multi-word phrase) it appears as a substring; or
     * (single token, treated as a stem) some word in the text starts with it. Stem/prefix
     * matching avoids substring false positives like "revolution" matching "evolution".
     */
    private boolean matches(String keyword, String haystack, Set<String> words) {
        if (keyword.indexOf(' ') >= 0) {
            return haystack.contains(keyword);
        }
        for (String word : words) {
            if (word.startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> tokenize(String haystack) {
        Set<String> words = new LinkedHashSet<>();
        for (String token : haystack.split("[^a-z0-9]+")) {
            if (!token.isBlank()) {
                words.add(token);
            }
        }
        return words;
    }

    private String buildHaystack(Event event) {
        StringBuilder sb = new StringBuilder();
        appendLower(sb, event.getText());
        appendLower(sb, event.getPageTitle());
        appendLower(sb, event.getExtract());
        return sb.toString();
    }

    private void appendLower(StringBuilder sb, String value) {
        if (value != null) {
            sb.append(' ').append(value.toLowerCase(Locale.ROOT)).append(' ');
        }
    }

    /** Exposed for the UI: feed-derived tags that aren't themes. */
    public static boolean isFeedTag(String tag) {
        return "births".equals(tag) || "deaths".equals(tag) || "holidays".equals(tag);
    }

    /** Whether a tag is a decade bucket like {@code "1960s"}. */
    public static boolean isDecadeTag(String tag) {
        if (tag == null || tag.length() < 3 || !tag.endsWith("s")) {
            return false;
        }
        String digits = tag.substring(0, tag.length() - 1);
        return digits.chars().allMatch(Character::isDigit);
    }
}
