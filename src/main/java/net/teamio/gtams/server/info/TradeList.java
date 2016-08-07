package net.teamio.gtams.server.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TradeList {

	public TradeList() {
		trades = new ArrayList<>();
	}

	public TradeList(List<Trade> list) {
		this.trades = list;
	}

	public TradeList(Collection<Trade> list) {
		this.trades = new ArrayList<>(list);
	}

	public List<Trade> trades;
}
