package net.teamio.gtams.server.info;

import java.util.UUID;

public class Trade {

	public UUID terminalId;

	public TradeDescriptor descriptor;
	public boolean isBuy;
	public int price;
	public int interval;
	public int stopAfter;
	public Mode mode = Mode.Once;

	/**
	 * @param itemName
	 * @param damage
	 * @param nbtHash
	 */
	public Trade(TradeDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public Trade() {

	}
}
