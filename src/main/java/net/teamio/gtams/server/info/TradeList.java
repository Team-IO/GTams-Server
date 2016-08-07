package net.teamio.gtams.server.info;

import java.util.ArrayList;
import java.util.Collection;

public class TradeList {
	public ArrayList<Trade> trades;

	public TradeList() {
		trades = new ArrayList<>();
	}

	public TradeList(ArrayList<Trade> list) {
		this.trades = list;
		if (trades == null) {
			trades = new ArrayList<>();
		}
	}

	public TradeList(Collection<Trade> list) {
		if (list == null) {
			this.trades = new ArrayList<>();
		} else {
			this.trades = new ArrayList<>(list);
		}
	}

}
