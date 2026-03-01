package org.archangel;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.archangel.rss.ArchNewsFetcher;
import jakarta.ws.rs.core.MediaType;

import jakarta.inject.Inject;
@Path("/rss-test")
public class Tester_1{
    @Inject
    private ArchNewsFetcher archNewsFetcher;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() throws Exception {
        return archNewsFetcher.fetchRSSNews().substring(0, 500);
    }
}
