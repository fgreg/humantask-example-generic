package com.example.bpms;

import java.lang.reflect.Constructor;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory;
import org.kie.api.runtime.manager.RuntimeEngine;

public class CamelAwareRegisterableItemsFactory extends DefaultRegisterableItemsFactory {

	private final CamelContext camel;
	
	public CamelAwareRegisterableItemsFactory(CamelContext camel){
		this.camel = camel;
	}
	
	@Override
	protected <T> T createInstance(Class<T> clazz, RuntimeEngine engine) {
		T instance = null;
		
		try {
            Constructor<T> constructor = clazz.getConstructor(ProducerTemplate.class);
            
            instance = constructor.newInstance(camel.createProducerTemplate());
        } catch (Exception e) {

        }
		
		if(instance == null){
			instance = super.createInstance(clazz, engine);
		}
		return instance;
	}
}
