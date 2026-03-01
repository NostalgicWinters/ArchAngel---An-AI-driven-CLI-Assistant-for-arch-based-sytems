package org.archangel.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.archangel.model.NewsItem;
import org.archangel.rss.ArchNewsFetcher;
import org.archangel.scheduler.NewsScheduler;
import org.archangel.state.NewsCacheService;

import java.util.List;
import java.util.stream.Collectors;

@Path("news")
public class NewsResource
{
  @Inject
  NewsCacheService newsCacheService;
  @GET
    public List<NewsItem> getNews(
            @QueryParam("limit") @DefaultValue("0") int limit,
            @QueryParam("keyword") String keyword)
  {
      List<NewsItem> news = newsCacheService.getNews();
      if (keyword != null && !keyword.isBlank()) {
          news = news.stream()
                  .filter(item ->
                          item.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                                  item.getDescription().toLowerCase().contains(keyword.toLowerCase())
                  )
                  .collect(Collectors.toList());
      }
      if (limit > 0 && limit < news.size()) {
          news = news.subList(0, limit);
      }
      return news;
  }
}
