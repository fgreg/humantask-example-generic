package com.example.camel;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.kie.api.runtime.manager.RuntimeManager;

import com.example.bpms.SupplyItem;
import com.example.bpms.SupplyItems;

public class SupplyItemRoute extends RouteBuilder {
	
	private RuntimeManager manager;
	
	public SupplyItemRoute(RuntimeManager manager){
		this.manager =  manager;
	}

	@Override
	public void configure() throws Exception {
		
		from("direct:supplyitem")
		//Unmarshal the incoming data into a POJO
		.unmarshal().json()
		//Transform from the SupplyItems object to a List<SupplyItem>
		.transform(new Expression(){
			@SuppressWarnings("unchecked")
			@Override
			public <List> List evaluate(Exchange exchange, Class<List> type) {
				return (List) exchange.getIn().getBody(SupplyItems.class).getItems();
			}
		})
		//Split the list of SupplyItems
		.split(body())
			//Enrich each SupplyItem with a cost
			.enrich("direct:itemcost")
			//Start the BPM Process
			.process(new SupplyItemApprovalProcessor(manager));
		
		final AtomicInteger approve = new AtomicInteger(0);
		//Simulate a call to an external service to get additional data
		from("direct:itemcost").process(new Processor() {
		    public void process(Exchange exchange) {
		        Message in = exchange.getIn();
		        if(approve.incrementAndGet() % 2 == 0){
		        	in.setBody(in.getBody(SupplyItem.class).setCost(10.00));
		        }else{
		        	in.setBody(in.getBody(SupplyItem.class).setCost(5.00));
		        }
		    }
		});
	}

}
