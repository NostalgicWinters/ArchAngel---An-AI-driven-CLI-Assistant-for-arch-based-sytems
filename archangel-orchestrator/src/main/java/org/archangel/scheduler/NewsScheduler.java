package org.archangel.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.archangel.rss.ArchNewsFetcher;
import org.archangel.state.NewsCacheService;
import org.jboss.logging.Logger;

/**
 * FIXED:
 * 1. recordFetchFailure() now called on error so NewsCacheService can track
 *    consecutive failures and callers can know the cache is degraded.
 *
 * 2. Escalating log level: after 3 consecutive failures, log at ERROR instead
 *    of INFO so monitoring systems (journald, log shippers) can alert on it.
 *
 * 3. Scheduler interval moved to config property — hardcoded "10m" made it
 *    impossible to tune without recompiling.
 */
@ApplicationScoped
public class NewsScheduler {

    private static final Logger log = Logger.getLogger(NewsScheduler.class);
    private static final int FAILURE_ESCALATION_THRESHOLD = 3;

    @Inject
    ArchNewsFetcher archNewsFetcher;

    @Inject
    NewsCacheService newsCacheService;

    @Scheduled(every = "${archangel.rss.refresh-interval:10m}", delayed = "5s")
    public void refreshNews() {
        try {
            var news = archNewsFetcher.fetchRSSNews();
            if (news != null && !news.isEmpty()) {
                newsCacheService.updateNews(news);
                log.infof("News cache refreshed: %d items", news.size());
            } else {
                log.warn("RSS feed returned empty list — cache not updated");
                newsCacheService.recordFetchFailure();
            }
        } catch (Exception e) {
            newsCacheService.recordFetchFailure();
            int failures = newsCacheService.getConsecutiveFailures();

            if (failures >= FAILURE_ESCALATION_THRESHOLD) {
                log.errorf("RSS feed has failed %d consecutive times: %s", failures, e.getMessage());
            } else {
                log.warnf("Failed to refresh RSS news (%d consecutive): %s", failures, e.getMessage());
            }
        }
    }
}