package org.archangel.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.archangel.entity.Incident;
import org.archangel.entity.Incident_Repo;

import java.util.List;

@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
public class IncidentResource {

    @Inject
    Incident_Repo incidentRepo;

    @GET
    @Path("/incidents")
    public List<Incident> getIncidents
            (@QueryParam("limit") @DefaultValue("50") int limit) {

        return incidentRepo.findAll()
                .page(0, limit)
                .list();
    }
}