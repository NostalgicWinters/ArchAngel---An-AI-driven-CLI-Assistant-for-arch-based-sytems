package org.archangel.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.archangel.entity.Incident;
import org.archangel.entity.Incident_Repo;
import org.archangel.model.SystemStatus;

@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
public class SystemStatusResource{
    @Inject
    Incident_Repo incidentRepo;

    @GET
    @Path("/status")
    public SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        status.setExecutionEnabled(true);
        status.setTotalIncidents(incidentRepo.count());

        Incident last = incidentRepo.findAll()
                .page(0,1)
                .firstResult();
        if(last != null) {
            status.setLastSeverity(last.getSeverity());
            status.setLastSummary(last.getSummary());
        }

        return status;
    }
}
