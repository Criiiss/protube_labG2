package com.tecnocampus.LS2.protube_back.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

/** One public playlist in the search index. */
@Document(SearchService.PLAYLISTS)
public record SearchPlaylist(@Id String id, String title, String owner, String description, long videoCount,
                             Instant createdAt, String titleNorm, List<String> terms) {

    public static SearchPlaylist of(String id, String title, String owner, String description, long videoCount, Instant createdAt) {
        return new SearchPlaylist(id, title, owner, description == null ? "" : description, videoCount, createdAt,
                SearchDocument.normalize(title), SearchDocument.terms(title + " " + owner));
    }
}
