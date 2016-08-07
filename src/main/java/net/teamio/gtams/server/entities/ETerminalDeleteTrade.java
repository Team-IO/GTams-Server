package net.teamio.gtams.server.entities;

import java.util.UUID;

public class ETerminalDeleteTrade {

	public UUID id;
	public long trade;

	public ETerminalDeleteTrade(UUID id, long trade) {
		this.id = id;
		this.trade = trade;
	}

}
