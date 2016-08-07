package net.teamio.gtams.server.info;

public class Goods {
	public TradeDescriptor what;
	public int amount;

	public Goods() {

	}

	public Goods(TradeDescriptor what, int amount) {
		this.what = what;
		this.amount = amount;
	}

	@Override
	public String toString() {
		return amount + " of " + what;
	}
}
