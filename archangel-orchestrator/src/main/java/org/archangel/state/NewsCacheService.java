package org.archangel.state;

import jakarta.enterprise.context.ApplicationScoped;
import org.archangel.model.NewsItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * FIXED:
 * 1. Added lastSuccessfulFetch timestamp — previously the cache had no concept of
 *    staleness. After an RSS failure the cache silently served week-old data with
 *    no indication to the caller that the data was stale.
 *
 * 2. isStale() exposed so NewsResource and the scheduler can surface cache age
 *    in API responses (via a custom header or response wrapper).
 *
 * 3. consecutiveFailures counter — allows the scheduler to escalate alerts
 *    after N consecutive RSS fetch failures rather than silently swallowing them.
 */
@ApplicationScoped
public class NewsCacheService {

    // 30 minutes — if cache is older than this, warn callers
    private static final long STALE_THRESHOLD_SECONDS = 1800;

    private List<NewsItem> cachedNews = new ArrayList<>();
    private Instant lastSuccessfulFetch = null;
    private int consecutiveFailures = 0;

    public synchronized void updateNews(List<NewsItem> news) {
        this.cachedNews = new ArrayList<>(news);
        this.lastSuccessfulFetch = Instant.now();
        this.consecutiveFailures = 0;
    }

    public synchronized void recordFetchFailure() {
        this.consecutiveFailures++;
    }

    public synchronized List<NewsItem> getNews() {
        return new ArrayList<>(cachedNews);
    }

    public synchronized Instant getLastSuccessfulFetch() {
        return lastSuccessfulFetch;
    }

    public synchronized boolean isStale() {
        if (lastSuccessfulFetch == null) return true;
        return Instant.now().getEpochSecond() - lastSuccessfulFetch.getEpochSecond() > STALE_THRESHOLD_SECONDS;
    }

    public synchronized int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public synchronized boolean hasData() {
        return !cachedNews.isEmpty();
    }
}