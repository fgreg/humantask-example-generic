package com.example.bpms;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.SystemException;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.h2.tools.Server;
import org.jbpm.services.task.audit.lifecycle.listeners.BAMTaskEventListener;
import org.jbpm.services.task.identity.MvelUserGroupCallbackImpl;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.task.UserGroupCallback;
import org.kie.internal.io.ResourceFactory;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

import com.example.bpms.audit.JPACustomAuditLogService;
import com.example.bpms.audit.CustomAuditLogService;
import com.example.camel.CamelWorkItemHandler;
import com.example.camel.NotificationRoute;
import com.example.camel.SupplyItemRoute;

/**
 * This is a utility class that keeps track of the components that will be running for each
 * example.
 * 
 * It is a singleton and the static get(); method should be used to get the instance.
 * 
 * The environment can be started by calling the start(...) method and passing in the location
 * of any BPM resources (DRL, BPMN, etc...) you want the manager to be aware of.
 * 
 * The environment can be stopped by calling the stop() method.
 * 
 * @author Frank
 *
 */
public class EnvironmentManager {

	private static EnvironmentManager INSTANCE = null;

	private EntityManagerFactory emf;
	private Server h2;
	private RuntimeManager manager;
	private CamelContext camelContext = new DefaultCamelContext();
	private static final String PERSISTENCE_UNIT_NAME = "org.jbpm.persistence.jpa";

	private EnvironmentManager() {

	}

	/**
	 * Used to get an instance of the Environment Manager
	 * 
	 * @return The Instance.
	 */
	public static EnvironmentManager get() {
		if (INSTANCE == null) {
			INSTANCE = new EnvironmentManager();
		}

		return INSTANCE;
	}

	/**
	 * Starts the components necessary (Bitronix, H2, Hibernate) to run the examples.
	 * 
	 * @param resources Any BPM resources (DRL, BPM, etc...) the manager should be aware of
	 * @return A configured RuntimeManager to be used by the examples.
	 */
	public RuntimeManager start(String... resources) {
		setupPersistence();
		setupManager(resources);
		startCamel(manager);
		return manager;
	}
	
	/**
	 * Custom Audit Log Service used for querying process information.
	 * 
	 * @return The audit log service.
	 */
	public CustomAuditLogService getAuditLogService(){
		CustomAuditLogService auditLogService = new JPACustomAuditLogService(emf);
		((JPACustomAuditLogService) auditLogService).setPersistenceUnitName(PERSISTENCE_UNIT_NAME);
		return auditLogService;
	}
	
	public CamelContext getCamelContext(){
		return camelContext;
	}
	
	/**
	 * Clean shutdown of the environment.
	 */
	public void stop(){
		cleanup();
	}

	private void setupPersistence() {
		h2 = startH2Server();
		setupDataSource();

		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("hibernate.hbm2ddl.auto", "create");
		configOverrides.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");

		emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, configOverrides);

		BitronixTransactionManager transactionManager = TransactionManagerServices.getTransactionManager();
		try {
			transactionManager.setTransactionTimeout(3600);
		} catch (SystemException e1) {
			e1.printStackTrace();
		}

	}

	private Server startH2Server() {
		try {
			// start h2 in memory database
			Server server = Server.createTcpServer(new String[0]);
			server.start();
			return server;
		} catch (Throwable t) {
			throw new RuntimeException("Could not start H2 server", t);
		}
	}

	private void startCamel(RuntimeManager manager) {
		
		try {
			camelContext.addRoutes(new SupplyItemRoute(manager));
			camelContext.addRoutes(new NotificationRoute());
			camelContext.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private PoolingDataSource setupDataSource() {
		Properties properties = new Properties();
		try {
			properties.load(EnvironmentManager.class.getResourceAsStream("/jBPM.properties"));
		} catch (Throwable t) {
			// do nothing, use defaults
		}
		// create data source
		PoolingDataSource pds = new PoolingDataSource();
		pds.setUniqueName(properties.getProperty("persistence.datasource.name", "jdbc/jbpm-ds"));
		pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
		pds.setMaxPoolSize(5);
		pds.setAllowLocalTransactions(true);
		pds.getDriverProperties().put("user", properties.getProperty("persistence.datasource.user", "sa"));
		pds.getDriverProperties().put("password", properties.getProperty("persistence.datasource.password", ""));
		pds.getDriverProperties().put("url", properties.getProperty("persistence.datasource.url", "jdbc:h2:tcp://localhost/~/jbpm-db;MVCC=TRUE"));
		pds.getDriverProperties().put("driverClassName", properties.getProperty("persistence.datasource.driverClassName", "org.h2.Driver"));
		pds.init();
		
		return pds;
	}

	private void cleanup() {
		
		try {
			manager.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		try {
			emf.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}

		try {
			h2.stop();
		} catch (Throwable t) {
			t.printStackTrace();
		}

		try {
			TransactionManagerServices.getTransactionManager().shutdown();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		try {
			if(camelContext !=null){
				camelContext.stop();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	private void setupManager(String... resources) {

		RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder();

		//Turn on task listener and register custom work item handlers
		CamelAwareRegisterableItemsFactory registerableItemsFactory = new CamelAwareRegisterableItemsFactory(camelContext);
	    registerableItemsFactory.addTaskListener(BAMTaskEventListener.class);
	    registerableItemsFactory.addWorkItemHandler("CamelRoute", CamelWorkItemHandler.class);

		builder.persistence(true)
			.entityManagerFactory(emf)
			.userGroupCallback(setupUserGroups())
			.registerableItemsFactory(registerableItemsFactory);
		for (String resource : resources) {
			builder.addAsset(ResourceFactory.newClassPathResource(resource), ResourceType.determineResourceType(resource));
		}

		RuntimeEnvironment environment = builder.get();

		manager = RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(environment);
	}

	private UserGroupCallback setupUserGroups() {
		return new MvelUserGroupCallbackImpl(true);
	}
}
