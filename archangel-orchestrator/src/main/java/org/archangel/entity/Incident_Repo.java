package org.archangel.entity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@ApplicationScoped
public class Incident_Repo implements PanacheRepository<Incident> {

}
