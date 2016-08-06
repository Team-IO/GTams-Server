package net.teamio.gtams.server.info;

import java.util.UUID;

public class Transaction {

	UUID vendor;
	UUID customer;
	TradeDescriptor descriptor;
	int amount;
	int price;

	public Transaction(UUID vendor, UUID customer, TradeDescriptor descriptor, int amount, int price) {
		this.vendor = vendor;
		this.customer = customer;
		this.descriptor = descriptor;
		this.amount = amount;
		this.price = price;
	}

}
