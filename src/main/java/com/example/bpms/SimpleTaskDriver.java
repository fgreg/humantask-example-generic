package com.example.bpms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.SystemException;

import org.h2.tools.Server;
import org.jbpm.services.task.identity.MvelUserGroupCallbackImpl;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.context.CorrelationKeyContext;
import org.kie.internal.runtime.manager.context.EmptyContext;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class SimpleTaskDriver {
	
	private static EntityManagerFactory emf;
	private static Server h2;

	public static void main(String[] args) {
		setupPersistence();
		try{
			new SimpleTaskDriver().start(setupManager());
		}finally{
			cleanup();
		}
		System.out.println("done.");
	}
	
	private static void setupPersistence() {
        h2 = startH2Server();
        setupDataSource();

        Map<String, Object> configOverrides = new HashMap<>();
        configOverrides.put("hibernate.hbm2ddl.auto", "create"); 
        configOverrides.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");

        emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa", configOverrides);
        
        BitronixTransactionManager transactionManager = TransactionManagerServices.getTransactionManager();
        try {
			transactionManager.setTransactionTimeout(3600);
		} catch (SystemException e1) {
			e1.printStackTrace();
		}

    }
	
	public static Server startH2Server() {
        try {
            // start h2 in memory database
            Server server = Server.createTcpServer(new String[0]);
            server.start();
            return server;
        } catch (Throwable t) {
            throw new RuntimeException("Could not start H2 server", t);
        }
    }
	
	public static PoolingDataSource setupDataSource() {
        Properties properties = new Properties();
        try {
            properties.load(SimpleTaskDriver.class.getResourceAsStream("/jBPM.properties"));
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
	
	private static void cleanup(){
		try{
			emf.close();
		}catch(Throwable t){
			t.printStackTrace();
		}
		
		try{
			h2.stop();
		}catch(Throwable t){
			t.printStackTrace();
		}
		
		try{
			TransactionManagerServices.getTransactionManager().shutdown();
		}catch(Throwable t){
			t.printStackTrace();
		}
		
	}
	
	private static RuntimeManager setupManager(){
		RuntimeEnvironment environment = RuntimeEnvironmentBuilder.Factory
				.get()
				.newDefaultBuilder()
				.persistence(true)
				.entityManagerFactory(emf)
				.userGroupCallback(setupUserGroups())
				.addAsset(
						ResourceFactory
								.newClassPathResource("com/example/bpms/simplesupplyitemapproval.bpmn2"),
						ResourceType.BPMN2)
				.get();
		
		return RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(environment);
	}
	
	private static UserGroupCallback setupUserGroups(){
        return new MvelUserGroupCallbackImpl(true);
	}

	
	public void start(final RuntimeManager manager){
		
		RuntimeEngine runtimeEngine = manager.getRuntimeEngine(EmptyContext.get());
		
		final SupplyItem item1 = new SupplyItem();
		item1.setDescription("Red Shoes");
		final SupplyItem item2 = new SupplyItem();
		item2.setDescription("Red Hat");
		
//		Doesn't work
		ExecutorService exec = Executors.newCachedThreadPool();
		
		
		exec.submit(new Runnable(){
			@Override
			public void run() {
				// start a new process instance
				RuntimeEngine runtimeEngine = manager.getRuntimeEngine(CorrelationKeyContext.get());
				KieSession ksession = runtimeEngine.getKieSession();
				Map<String, Object> params = new HashMap<>();
				params.put("supplyItem", item1);
				ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
				
				System.out.println("Started process:" + proc);
				
				manager.disposeRuntimeEngine(runtimeEngine);
			}
        });
		exec.submit(new Runnable(){
			@Override
			public void run() {
				// start a new process instance
				RuntimeEngine runtimeEngine = manager.getRuntimeEngine(CorrelationKeyContext.get());
				KieSession ksession = runtimeEngine.getKieSession();
				Map<String, Object> params = new HashMap<>();
				params.put("supplyItem", item2);
				ProcessInstance proc = ksession.startProcess("com.example.bpms.simplesupplyitemapproval", params);
				
				System.out.println("Started process:" + proc);
				
				manager.disposeRuntimeEngine(runtimeEngine);
			}
        });
		
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Doesn't work
//		startSubmittask(item1, runtimeEngine);
//		startSubmittask(item2, runtimeEngine);
		
		//Works
//		startSubmittask(item1, manager.getRuntimeEngine(EmptyContext.get()));
//		startSubmittask(item2, manager.getRuntimeEngine(EmptyContext.get()));
		
		//Doesn't Work
//		ExecutorService exec = Executors.newCachedThreadPool();
//		
//		exec.submit(new CopyOfSimpleApproval(item1, runtimeEngine));
//		exec.submit(new CopyOfSimpleApproval(item2, runtimeEngine));
//		
//		exec.shutdown();
//		try {
//			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		
		
		TaskService taskService = runtimeEngine.getTaskService();
        // ------------
        {
            List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("bob", "en-UK");
            for (TaskSummary taskSummary : list) {
                System.out.println("bob starts a task : taskId = " + taskSummary.getId());
                taskService.start(taskSummary.getId(), "bob");
                Map<String,Object> content = taskService.getTaskContent(taskSummary.getId());
                System.out.println(content);
                SupplyItem taskItem = (SupplyItem) content.get("supplyItemInput");
                if(taskItem.getDescription().equals("Red Hat")){
                	content.put("approved", true);
                }else{
                	content.put("approved", false);
                }
                taskService.complete(taskSummary.getId(), "bob", content);
            }
        }
        
        System.out.println("Completed tasks: " + taskService.getTasksAssignedAsPotentialOwnerByStatus("bob", Arrays.asList(Status.Completed), "en-UK"));

        // -----------
        manager.disposeRuntimeEngine(runtimeEngine);
        manager.close();
	}
	
	public ProcessInstance startSubmittask(SupplyItem item, RuntimeEngine runtimeEngine) {
		KieSession ksession = runtimeEngine.getKieSession();
		Map<String, Object> params = new HashMap<>();
		params.put("supplyItem", item);
		ProcessInstance proc = ksession.startProcess(
				"com.example.bpms.simplesupplyitemapproval", params);

		System.out.println("Started process:" + proc);

		return proc;
	}
	
	
}
