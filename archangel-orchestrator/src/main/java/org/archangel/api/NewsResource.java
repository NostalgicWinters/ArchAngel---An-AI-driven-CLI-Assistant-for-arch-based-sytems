package org.archangel.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.archangel.model.NewsItem;
import org.archangel.state.NewsCacheService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FIXED:
 * 1. Returns a proper Response object so we can attach cache staleness headers.
 *    Previously the CLI had no way to know it was receiving stale data.
 *
 * 2. Returns 503 with a clear message when the cache is empty (cold start),
 *    instead of silently returning [] which the CLI would silently display as
 *    "No news items found" — confusing to the user.
 *
 * 3. X-Cache-Stale header added: CLI can check this and show a warning like
 *    "[WARNING: news data may be stale]" without polling the server.
 *
 * 4. @Produces annotation added — was missing from the original, relying on
 *    Quarkus default content negotiation which can return XML in some clients.
 */
@Path("/news")
@Produces(MediaType.APPLICATION_JSON)
public class NewsResource {

    @Inject
    NewsCacheService newsCacheService;

    @GET
    public Response getNews(
            @QueryParam("limit") @DefaultValue("0") int limit,
            @QueryParam("keyword") String keyword) {

        if (!newsCacheService.hasData()) {
            return Response.status(503)
                    .entity("{\"error\": \"News cache is empty. Service may be starting up — retry in 10 seconds.\"}")
                    .build();
        }

        List<NewsItem> news = newsCacheService.getNews();

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.toLowerCase();
            news = news.stream()
                    .filter(item ->
                            item.getTitle().toLowerCase().contains(lowerKeyword) ||
                                    item.getDescription().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        if (limit > 0 && limit < news.size()) {
            news = news.subList(0, limit);
        }

        Instant lastFetch = newsCacheService.getLastSuccessfulFetch();
        boolean stale = newsCacheService.isStale();

        return Response.ok(news)
                .header("X-Cache-Stale", String.valueOf(stale))
                .header("X-Cache-Last-Updated", lastFetch != null ? lastFetch.toString() : "never")
                .build();
    }
}