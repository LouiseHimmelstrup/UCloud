package org.esciencecloud.facade.escienceclouddb;

import org.esciencecloud.jpa.escienceclouddb.Irodsresourcetype;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class IrodsresourcetypeFacade {
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IrodsresourcetypeFacade.class);

    public boolean createIrodsresourcetype(Irodsresourcetype entity) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("eScienceCloudJPAPU");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.close();
        emf.close();
        return true;
    }

    public boolean updateIrodsresourcetype(Irodsresourcetype entity) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("eScienceCloudJPAPU");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.merge(entity);
        em.getTransaction().commit();
        em.close();
        emf.close();
        return true;
    }

    public boolean deleteIrodsresourcetypeById(Irodsresourcetype entity) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("eScienceCloudJPAPU");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.remove(entity);
        em.getTransaction().commit();
        em.close();
        emf.close();
        return true;
    }

    public List<Irodsresourcetype> listAllIrodsresourcetype() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("eScienceCloudJPAPU");
        EntityManager em = emf.createEntityManager();
        List<Irodsresourcetype> entityList = new ArrayList<>();
        Query nq1 = em.createNamedQuery("Irodsresourcetype.findAll");
        entityList.addAll(nq1.getResultList());
        em.close();
        emf.close();
        return entityList;
    }
}