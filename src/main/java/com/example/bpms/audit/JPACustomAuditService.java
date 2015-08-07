package com.example.bpms.audit;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.process.audit.strategy.PersistenceStrategy;
import org.jbpm.process.audit.strategy.PersistenceStrategyType;
import org.jbpm.process.audit.strategy.StandaloneJtaStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom AuditLogService for querying the jBPM Audit Tables
 * 
 * @author Frank
 *
 */
public class JPACustomAuditService implements CustomAuditService  {
	
private static final Logger logger = LoggerFactory.getLogger(JPACustomAuditService.class);
    
    private PersistenceStrategy persistenceStrategy;
    
    private String persistenceUnitName = "org.jbpm.persistence.jpa";
    
    public JPACustomAuditService() {
        EntityManagerFactory emf = null;
        try { 
           emf = Persistence.createEntityManagerFactory(persistenceUnitName); 
        } catch( Exception e ) { 
           logger.info("The '" + persistenceUnitName + "' peristence unit is not available, no persistence strategy set for " + this.getClass().getSimpleName());
        }
        if( emf != null ) { 
            persistenceStrategy = new StandaloneJtaStrategy(emf);
        }
    }
    
    public JPACustomAuditService(Environment env, PersistenceStrategyType type) {
        persistenceStrategy = PersistenceStrategyType.getPersistenceStrategy(type, env);
    }
    
    public JPACustomAuditService(Environment env) {
        EntityManagerFactory emf = (EntityManagerFactory) env.get(EnvironmentName.ENTITY_MANAGER_FACTORY);
        if( emf != null ) { 
            persistenceStrategy = new StandaloneJtaStrategy(emf);
        } else { 
            persistenceStrategy = new StandaloneJtaStrategy(Persistence.createEntityManagerFactory(persistenceUnitName));
        } 
    }
    
    public JPACustomAuditService(EntityManagerFactory emf){
        persistenceStrategy = new StandaloneJtaStrategy(emf);
    }
    
    public JPACustomAuditService(EntityManagerFactory emf, PersistenceStrategyType type){
        persistenceStrategy = PersistenceStrategyType.getPersistenceStrategy(type, emf);
    }
    
    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#setPersistenceUnitName(java.lang.String)
     */
    public void setPersistenceUnitName(String persistenceUnitName) {
        persistenceStrategy = new StandaloneJtaStrategy(Persistence.createEntityManagerFactory(persistenceUnitName));
        this.persistenceUnitName = persistenceUnitName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findProcessInstances()
     */
    
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstanceLog> findProcessInstances() {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = em.createQuery("FROM ProcessInstanceLog").getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findProcessInstances(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstanceLog> findProcessInstances(String processId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = em
            .createQuery("FROM ProcessInstanceLog p WHERE p.processId = :processId")
                .setParameter("processId", processId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findActiveProcessInstances(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstanceLog> findActiveProcessInstances(String processId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processId = :processId AND p.end is null")
                .setParameter("processId", processId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findProcessInstance(long)
     */
    @Override
    public ProcessInstanceLog findProcessInstance(long processInstanceId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        try {
        	return (ProcessInstanceLog) getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processInstanceId = :processInstanceId")
                .setParameter("processInstanceId", processInstanceId).getSingleResult();
        } catch (NoResultException e) {
        	return null;
        } finally {
        	closeEntityManager(em, newTx);
        }
    }
    
    @Override
    public List<ProcessInstanceLog> findByProcessInstanceId(long processInstanceId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        try {
        	return getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processInstanceId = :processInstanceId order by id desc", ProcessInstanceLog.class)
                .setParameter("processInstanceId", processInstanceId).getResultList();
        } catch (NoResultException e) {
        	return null;
        } finally {
        	closeEntityManager(em, newTx);
        }
    }
    
    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findSubProcessInstances(long)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstanceLog> findSubProcessInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.parentProcessInstanceId = :processInstanceId")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findNodeInstances(long)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<NodeInstanceLog> findNodeInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<NodeInstanceLog> result = getEntityManager()
            .createQuery("FROM NodeInstanceLog n WHERE n.processInstanceId = :processInstanceId ORDER BY date,id")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findNodeInstances(long, java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<NodeInstanceLog> findNodeInstances(long processInstanceId, String nodeId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<NodeInstanceLog> result = getEntityManager()
            .createQuery("FROM NodeInstanceLog n WHERE n.processInstanceId = :processInstanceId AND n.nodeId = :nodeId ORDER BY date,id")
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("nodeId", nodeId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findVariableInstances(long)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceLog> findVariableInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<VariableInstanceLog> result = getEntityManager()
            .createQuery("FROM VariableInstanceLog v WHERE v.processInstanceId = :processInstanceId ORDER BY date")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#findVariableInstances(long, java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceLog> findVariableInstances(long processInstanceId, String variableId) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        List<VariableInstanceLog> result = em
            .createQuery("FROM VariableInstanceLog v WHERE v.processInstanceId = :processInstanceId AND v.variableId = :variableId ORDER BY date")
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("variableId", variableId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceLog> findVariableInstancesByName(String variableId, boolean onlyActiveProcesses) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        Query query;
        if( ! onlyActiveProcesses ) { 
             query = em.createQuery("FROM VariableInstanceLog v WHERE v.variableId = :variableId ORDER BY date");
        } else { 
            query = em.createQuery(
                    "SELECT v "
                    + "FROM VariableInstanceLog v, ProcessInstanceLog p "
                    + "WHERE v.processInstanceId = p.processInstanceId "
                    + "AND v.variableId = :variableId "
                    + "AND p.end is null "
                    + "ORDER BY v.date");
        }
        List<VariableInstanceLog> result = query.setParameter("variableId", variableId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceLog> findVariableInstancesByNameAndValue(String variableId, String value, boolean onlyActiveProcesses) {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        Query query;
        if( ! onlyActiveProcesses ) { 
             query = em.createQuery("FROM VariableInstanceLog v WHERE v.variableId = :variableId AND v.value = :value ORDER BY date");
        } else { 
            query = em.createQuery(
                    "SELECT v "
                    + "FROM VariableInstanceLog v, ProcessInstanceLog p "
                    + "WHERE v.processInstanceId = p.processInstanceId "
                    + "AND v.variableId = :variableId "
                    + "AND v.value = :value "
                    + "AND p.end is null "
                    + "ORDER BY v.date");
        }
        List<VariableInstanceLog> result = query
                .setParameter("variableId", variableId)
                .setParameter("value", value)
                .getResultList();
        closeEntityManager(em, newTx);
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#clear()
     */
    @Override
    @SuppressWarnings("unchecked")
    public void clear() {
        EntityManager em = getEntityManager();
        Object newTx = joinTransaction(em);
        
        List<ProcessInstanceLog> processInstances = em.createQuery("FROM ProcessInstanceLog").getResultList();
        for (ProcessInstanceLog processInstance: processInstances) {
            em.remove(processInstance);
        }
        List<NodeInstanceLog> nodeInstances = em.createQuery("FROM NodeInstanceLog").getResultList();
        for (NodeInstanceLog nodeInstance: nodeInstances) {
            em.remove(nodeInstance);
        }
        List<VariableInstanceLog> variableInstances = em.createQuery("FROM VariableInstanceLog").getResultList();
        for (VariableInstanceLog variableInstance: variableInstances) {
            em.remove(variableInstance);
        }           
        closeEntityManager(em, newTx);
    }

    /* (non-Javadoc)
     * @see org.jbpm.process.audit.AuditLogService#dispose()
     */
    @Override
    public void dispose() {
        persistenceStrategy.dispose();
    }

    private EntityManager getEntityManager() {
        return persistenceStrategy.getEntityManager();
    }

    private Object joinTransaction(EntityManager em) {
        return persistenceStrategy.joinTransaction(em);
    }

    private void closeEntityManager(EntityManager em, Object transaction) {
       persistenceStrategy.leaveTransaction(em, transaction);
    }
	
}
