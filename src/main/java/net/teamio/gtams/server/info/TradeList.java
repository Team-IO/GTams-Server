package net.teamio.gtams.server.info;

import java.util.ArrayList;

public class TradeList {

	public TradeList() {
		trades = new ArrayList<>();
	}

	public TradeList(ArrayList<Trade> trades) {
		this.trades = trades;
	}

	public ArrayList<Trade> trades;
}
