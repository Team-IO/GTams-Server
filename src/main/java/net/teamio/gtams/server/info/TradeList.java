package net.teamio.gtams.server.info;

import java.util.ArrayList;
import java.util.List;

public class TradeList {

	public TradeList() {
		trades = new ArrayList<>();
	}

	public TradeList(List<Trade> list) {
		this.trades = list;
	}

	public List<Trade> trades;
}
