package com.tecnocampus.LS2.protube_back.search;

import com.mongodb.MongoNamespace;
import com.mongodb.client.model.RenameCollectionOptions;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class SearchService {
    static final String VIDEOS = "search_videos";
    static final String PLAYLISTS = "search_playlists";
    private static final Map<String, Integer> DATE_DAYS = Map.of("today", 1, "week", 7, "month", 30, "year", 365);
    private static final int PER_TYPE_IN_MIXED_RESULTS = 4;

    private final MongoTemplate mongo;
    public SearchService(MongoTemplate mongo) { this.mongo = mongo; }

    /** Unknown filter values fall back to their defaults, so a bad URL never errors. */
    public record Params(String q, String type, String date, String duration, String sort, String channel, int page, int size) {
        public Params {
            q = q == null ? "" : q.strip();
            if (q.length() > 100) q = q.substring(0, 100);
            type = Set.of("video", "channel", "playlist").contains(type) ? type : "all";
            date = DATE_DAYS.containsKey(date) ? date : "any";
            duration = Set.of("short", "medium", "long").contains(duration) ? duration : "any";
            sort = Set.of("date", "views").contains(sort) ? sort : "relevance";
            channel = channel == null || channel.isBlank() ? null : channel;
            page = Math.max(0, Math.min(page, 1000));
            size = Math.max(1, Math.min(size, 48));
        }
    }
    public record VideoHit(String id, String title, String channel, String views, String published, String duration,
                           String categoryId, String thumbnail, long viewCount, String createdAt, List<String> tags) {}
    public record ChannelHit(String name, long videoCount, long viewCount) {}
    public record PlaylistHit(String id, String title, String owner, long videoCount) {}
    /** correctedQuery is set when nothing matched and the results are for a corrected spelling instead. */
    public record Result(String query, List<VideoHit> videos, long videoTotal, List<ChannelHit> channels, long channelTotal,
                         List<PlaylistHit> playlists, long playlistTotal, int page, boolean hasMore, boolean approximate,
                         String correctedQuery) {
        boolean isEmpty() { return videoTotal == 0 && channelTotal == 0 && playlistTotal == 0; }
    }

    public Result search(Params p) {
        Result exact = run(p, p.q());
        if (p.q().isEmpty() || !exact.isEmpty()) return exact;
        String corrected = correct(p.q());
        if (corrected == null) return exact;
        Result fixed = run(p, corrected);
        return new Result(p.q(), fixed.videos(), fixed.videoTotal(), fixed.channels(), fixed.channelTotal(), fixed.playlists(),
                fixed.playlistTotal(), p.page(), fixed.hasMore(), true, corrected);
    }

    private Result run(Params p, String q) {
        boolean mixed = p.type().equals("all");
        // Mixed results only show a few channels and playlists, for a typed query, on its first page
        boolean extras = mixed && p.page() == 0 && !q.isEmpty() && p.channel() == null;

        List<VideoHit> videos = List.of();
        long videoTotal = 0;
        if (mixed || p.type().equals("video")) {
            Query query = textOrAll(q);
            filters(p).forEach(query::addCriteria);
            videoTotal = mongo.count(query, VIDEOS);
            order(query, p.sort(), "views");
            query.skip((long) p.page() * p.size()).limit(p.size());
            videos = mongo.find(query, SearchDocument.class, VIDEOS).stream().map(SearchService::hit).toList();
        }

        List<ChannelHit> channels = List.of();
        long channelTotal = 0;
        if (extras || p.type().equals("channel")) {
            Criteria match = Criteria.where("channelNorm").regex(Pattern.quote(SearchDocument.normalize(q)));
            channelTotal = countChannels(match);
            channels = extras ? channels(match, 0, PER_TYPE_IN_MIXED_RESULTS) : channels(match, p.page() * p.size(), p.size());
        }

        List<PlaylistHit> playlists = List.of();
        long playlistTotal = 0;
        if (extras || p.type().equals("playlist")) {
            Query query = textOrAll(q);
            playlistTotal = mongo.count(query, PLAYLISTS);
            order(query, p.sort(), "videoCount");
            if (extras) query.limit(PER_TYPE_IN_MIXED_RESULTS);
            else query.skip((long) p.page() * p.size()).limit(p.size());
            playlists = mongo.find(query, SearchPlaylist.class, PLAYLISTS).stream()
                    .map(pl -> new PlaylistHit(pl.id(), pl.title(), pl.owner(), pl.videoCount())).toList();
        }

        long total = switch (p.type()) {
            case "channel" -> channelTotal;
            case "playlist" -> playlistTotal;
            default -> videoTotal;
        };
        return new Result(q, videos, videoTotal, channels, channelTotal, playlists, playlistTotal, p.page(),
                (long) (p.page() + 1) * p.size() < total, false, null);
    }

    /** Titles and channel names containing a word that starts with what the user typed. */
    public List<String> suggest(String q) {
        String prefix = SearchDocument.normalize(q);
        if (prefix.isEmpty()) return List.of();
        String wordStart = "(^| )" + Pattern.quote(prefix);
        Set<String> out = new LinkedHashSet<>();
        mongo.find(new Query(Criteria.where("titleNorm").regex(wordStart)).with(Sort.by(Sort.Direction.DESC, "views")).limit(6),
                SearchDocument.class, VIDEOS).forEach(doc -> out.add(doc.title()));
        mongo.findDistinct(new Query(Criteria.where("channelNorm").regex(wordStart)), "channel", VIDEOS, String.class)
                .stream().limit(3).forEach(out::add);
        return out.stream().limit(8).toList();
    }

    /** Swaps both collections at once (build into a temp collection, then rename) so searches never see them half-built. */
    public void replaceAll(Collection<SearchDocument> videos, Collection<SearchPlaylist> playlists) {
        swap(VIDEOS, videos, TextIndexDefinition.builder().onField("title", 10f).onField("tags", 5f)
                .onField("channel", 3f).onField("description", 1f).withDefaultLanguage("spanish").build());
        swap(PLAYLISTS, playlists, TextIndexDefinition.builder().onField("title", 10f).onField("owner", 3f)
                .onField("description", 1f).withDefaultLanguage("spanish").build());
    }

    private void swap(String collection, Collection<?> docs, TextIndexDefinition index) {
        String tmp = collection + "_tmp";
        mongo.dropCollection(tmp);
        mongo.indexOps(tmp).ensureIndex(index);
        if (!docs.isEmpty()) mongo.insert(docs, tmp);
        mongo.getCollection(tmp).renameCollection(new MongoNamespace(mongo.getDb().getName(), collection),
                new RenameCollectionOptions().dropTarget(true));
    }

    /** Each word is replaced by the closest indexed word, or null if nothing changes ("guitrra" -> "guitarra"). */
    String correct(String q) {
        Set<String> vocabulary = new HashSet<>(mongo.findDistinct(new Query(), "terms", VIDEOS, String.class));
        vocabulary.addAll(mongo.findDistinct(new Query(), "terms", PLAYLISTS, String.class));
        String original = SearchDocument.normalize(q);
        String fixed = Arrays.stream(original.split(" ")).map(word -> closest(word, vocabulary)).collect(Collectors.joining(" "));
        return fixed.equals(original) ? null : fixed;
    }

    /** Completes a cut word ("guit" -> "guitarra"), else allows 1 typo in words of 4-7 letters and 2 in longer ones. */
    static String closest(String word, Collection<String> vocabulary) {
        if (word.length() < 3 || vocabulary.contains(word)) return word;
        Comparator<String> shortest = Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder());
        Optional<String> completion = vocabulary.stream().filter(term -> term.startsWith(word)).min(shortest);
        if (completion.isPresent()) return completion.get();
        int allowed = word.length() >= 8 ? 2 : word.length() >= 4 ? 1 : 0;
        return vocabulary.stream()
                .filter(term -> Math.abs(term.length() - word.length()) <= allowed && distance(word, term) <= allowed)
                .min(Comparator.comparing((String term) -> distance(word, term)).thenComparing(shortest))
                .orElse(word);
    }

    /** Edit distance where swapping two neighbour letters also counts as one typo. */
    static int distance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
            }
        return d[a.length()][b.length()];
    }

    private static Query textOrAll(String q) {
        return q.isEmpty() ? new Query() : TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(q));
    }

    private static void order(Query query, String sort, String popularity) {
        if (sort.equals("relevance") && query instanceof TextQuery text) text.sortByScore();
        query.with(Sort.by(Sort.Direction.DESC, sort.equals("date") ? "createdAt" : popularity).and(Sort.by("_id")));
    }

    private static List<Criteria> filters(Params p) {
        List<Criteria> out = new ArrayList<>();
        if (DATE_DAYS.containsKey(p.date()))
            out.add(Criteria.where("createdAt").gte(Instant.now().minus(Duration.ofDays(DATE_DAYS.get(p.date())))));
        switch (p.duration()) {
            case "short" -> out.add(Criteria.where("durationSec").lt(240));
            case "medium" -> out.add(Criteria.where("durationSec").gte(240).lte(1200));
            case "long" -> out.add(Criteria.where("durationSec").gt(1200));
            default -> { }
        }
        if (p.channel() != null) out.add(Criteria.where("channel").is(p.channel()));
        return out;
    }

    private long countChannels(Criteria match) {
        Document row = mongo.aggregate(newAggregation(match(match), group("channel"), count().as("total")),
                VIDEOS, Document.class).getUniqueMappedResult();
        return row == null ? 0 : ((Number) row.get("total")).longValue();
    }

    private List<ChannelHit> channels(Criteria match, int skip, int limit) {
        Aggregation aggregation = newAggregation(match(match),
                group("channel").count().as("videoCount").sum("views").as("viewCount"),
                sort(Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by("_id"))), skip(skip), limit(limit));
        return mongo.aggregate(aggregation, VIDEOS, Document.class).getMappedResults().stream()
                .map(row -> new ChannelHit(row.getString("_id"), ((Number) row.get("videoCount")).longValue(),
                        ((Number) row.get("viewCount")).longValue()))
                .toList();
    }

    public static VideoHit hit(SearchDocument doc) {
        long seconds = Math.round(doc.durationSec());
        String duration = seconds >= 3600
                ? String.format("%d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60)
                : String.format("%d:%02d", seconds / 60, seconds % 60);
        return new VideoHit(doc.id(), doc.title(), doc.channel(), doc.views() + " visualizaciones",
                ago(doc.createdAt()), duration, "all", doc.thumbnail(), doc.views(), doc.createdAt().toString(), doc.tags());
    }

    static String ago(Instant when) {
        long days = Math.max(0, Duration.between(when, Instant.now()).toDays());
        if (days == 0) return "hoy";
        if (days < 30) return days == 1 ? "hace 1 día" : "hace " + days + " días";
        if (days < 365) return days < 60 ? "hace 1 mes" : "hace " + days / 30 + " meses";
        return days < 730 ? "hace 1 año" : "hace " + days / 365 + " años";
    }
}
