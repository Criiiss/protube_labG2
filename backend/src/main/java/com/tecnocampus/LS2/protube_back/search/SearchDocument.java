package com.tecnocampus.LS2.protube_back.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * One searchable video in Mongo. titleNorm/channelNorm are lowercase, accent-free copies for prefix matching;
 * terms are the distinct words used to correct typos.
 */
@Document(SearchService.VIDEOS)
public record SearchDocument(@Id String id, String title, String channel, List<String> tags, String description,
                             double durationSec, long views, Instant createdAt, String thumbnail,
                             String titleNorm, String channelNorm, List<String> terms) {

    public static SearchDocument of(String id, String title, String channel, List<String> tags, String description,
                                    double durationSec, long views, Instant createdAt, String thumbnail) {
        List<String> safeTags = tags == null ? List.of() : tags;
        return new SearchDocument(id, title, channel, safeTags, description == null ? "" : description, durationSec,
                views, createdAt, thumbnail, normalize(title), normalize(channel),
                terms(title + " " + channel + " " + String.join(" ", safeTags)));
    }

    /** "¡Música Épica!" -> "musica epica" */
    public static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    static List<String> terms(String text) {
        return Arrays.stream(normalize(text).split(" ")).filter(word -> word.length() >= 3).distinct().toList();
    }
}
