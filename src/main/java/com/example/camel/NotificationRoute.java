package com.example.camel;

import org.apache.camel.builder.RouteBuilder;

public class NotificationRoute extends RouteBuilder {

	
	@Override
	public void configure() throws Exception {
		
		from("direct:notifyItemApproval")
		.transform().simple("Camel says item approved: ${in.body}")
		.to("stream:out");
		
	}
}
