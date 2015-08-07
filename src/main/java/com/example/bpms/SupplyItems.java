package com.example.bpms;

import java.util.ArrayList;
import java.util.List;

public class SupplyItems {

	private List<SupplyItem> items;

	public List<SupplyItem> getItems() {
		return new ArrayList<SupplyItem>(items);
	}

	public void setItems(List<SupplyItem> items) {
		this.items = new ArrayList<SupplyItem>(items);
	}
}
