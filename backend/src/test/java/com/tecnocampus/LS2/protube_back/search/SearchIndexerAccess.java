package com.tecnocampus.LS2.protube_back.search;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

/** Lets tests in other packages read what the indexer would index, without making that API public. */
public final class SearchIndexerAccess {
    private SearchIndexerAccess() { }
    public static List<SearchDocument> videos(JdbcTemplate jdbc) { return new SearchIndexer(jdbc, null).videos(); }
    public static List<SearchPlaylist> playlists(JdbcTemplate jdbc) { return new SearchIndexer(jdbc, null).playlists(); }
}
