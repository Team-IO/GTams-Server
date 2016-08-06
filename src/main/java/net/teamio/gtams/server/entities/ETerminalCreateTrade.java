package net.teamio.gtams.server.entities;

import java.util.UUID;

import net.teamio.gtams.server.info.Trade;

public class ETerminalCreateTrade {

	public UUID id;
	public Trade trade;

	/**
	 * @param id
	 * @param trade
	 */
	public ETerminalCreateTrade(UUID id, Trade trade) {
		super();
		this.id = id;
		this.trade = trade;
	}

}
