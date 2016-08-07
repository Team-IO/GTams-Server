package net.teamio.gtams.server.entities;

import java.util.UUID;

import net.teamio.gtams.server.info.Trade;

public class ETerminalCreateTrade {

	public UUID id;
	public Trade trade;

	public ETerminalCreateTrade(UUID id, Trade trade) {
		this.id = id;
		this.trade = trade;
	}

}
