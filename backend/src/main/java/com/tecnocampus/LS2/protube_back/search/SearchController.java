package com.tecnocampus.LS2.protube_back.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** US-08: public search over the Mongo index. */
@RestController
@RequestMapping("/api/search")
public class SearchController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);
    private final SearchService search;
    public SearchController(SearchService search) { this.search = search; }

    @GetMapping
    public SearchService.Result search(@RequestParam(defaultValue = "") String q,
                                       @RequestParam(defaultValue = "all") String type,
                                       @RequestParam(defaultValue = "any") String date,
                                       @RequestParam(defaultValue = "any") String duration,
                                       @RequestParam(defaultValue = "relevance") String sort,
                                       @RequestParam(required = false) String channel,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "12") int size) {
        return search.search(new SearchService.Params(q, type, date, duration, sort, channel, page, size));
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam(defaultValue = "") String q) { return search.suggest(q); }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> unavailable(DataAccessException ex) {
        LOG.warn("Búsqueda no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "El buscador no está disponible ahora mismo. Inténtalo en unos minutos."));
    }
}
