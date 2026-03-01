package org.archangel.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.archangel.rss.ArchNewsFetcher;
import org.archangel.state.NewsCacheService;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NewsScheduler
{

    private static final Logger log = Logger.getLogger(NewsScheduler.class);

    @Inject
    ArchNewsFetcher  archNewsFetcher;

    @Inject
    NewsCacheService  newsCacheService;

    @Scheduled(every = "10m" , delayed = "5s")
    public void refreshNews()
    {
        try {
            var news = archNewsFetcher.fetchRSSNews();
            if (news != null && !news.isEmpty()) {
                newsCacheService.updateNews(news);
                log.info("News cache refreshed");
            }
        }
        catch (Exception e) {
            log.error("Failed to refresh news: " + e.getMessage());
        }
    }
}
