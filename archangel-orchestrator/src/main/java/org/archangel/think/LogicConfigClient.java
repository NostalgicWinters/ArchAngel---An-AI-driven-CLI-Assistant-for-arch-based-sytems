package org.archangel.think;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@Path("/config")
@RegisterRestClient(configKey = "logic-api")
public interface LogicConfigClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> getConfig();
}