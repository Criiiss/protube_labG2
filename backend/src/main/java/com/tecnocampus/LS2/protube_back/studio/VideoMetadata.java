package com.tecnocampus.LS2.protube_back.studio;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import java.time.Instant;
import java.util.*;

public record VideoMetadata(String title, String description, List<String> tags, String category, String language,
                            boolean ageRestricted, boolean commentsAllowed, Visibility visibility,
                            Instant publishAt, boolean draft, String thumbnail) {
    public enum Visibility { PUBLIC, PRIVATE, UNLISTED }

    VideoMetadata validated() {
        if (title == null || title.isBlank() || title.strip().length() > 100)
            throw invalid("El título debe tener entre 1 y 100 caracteres.");
        if (description != null && description.length() > 5000)
            throw invalid("La descripción admite hasta 5000 caracteres.");
        if (tags != null && (tags.size() > 20 || tags.stream().anyMatch(tag -> tag == null || tag.strip().length() > 40)))
            throw invalid("Introduce como máximo 20 etiquetas de hasta 40 caracteres.");
        if (category == null || !Set.of("all", "music", "gaming", "sports", "technology", "podcasts", "travel", "culture").contains(category))
            throw invalid("Selecciona una categoría válida.");
        if (language == null || !language.matches("und|[a-z]{2,3}(-[A-Z]{2})?"))
            throw invalid("Selecciona un idioma válido.");
        if (visibility == null) throw invalid("Selecciona la visibilidad del vídeo.");
        if (publishAt != null && (visibility != Visibility.PUBLIC || draft))
            throw invalid("Solo se pueden programar vídeos públicos que no sean borradores.");
        String chosen = thumbnail == null || thumbnail.isBlank() ? "auto-1" : thumbnail;
        if (!Set.of("auto-1", "auto-2", "auto-3", "custom").contains(chosen))
            throw invalid("Selecciona una miniatura válida.");
        return new VideoMetadata(title.strip(), Objects.toString(description, "").strip(),
                tags == null ? List.of() : tags.stream().map(String::strip).filter(tag -> !tag.isEmpty()).distinct().toList(),
                category, language, ageRestricted, commentsAllowed, visibility, publishAt, draft, chosen);
    }

    boolean published(Instant now) {
        return !draft && (publishAt == null || !publishAt.isAfter(now));
    }

    private static AuthFailure invalid(String message) {
        return new AuthFailure(400, message);
    }
}
