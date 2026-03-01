package org.archangel.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.archangel.model.CommandResponse;
import org.archangel.system.CommandService;

@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.TEXT_PLAIN)
public class SystemResource {

    @Inject
    CommandService commandService;

    @POST
    @Path("/execute")
    public CommandResponse executeCommand(String command) {
        return commandService.executeCommand(command);
    }
}