package net.teamio.gtams.server.entities;

public class TradeDescriptor {

	public String itemName;
	public int damage;
	public String nbtHash;

	public TradeDescriptor() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param itemName
	 * @param damage
	 * @param nbtHash
	 */
	public TradeDescriptor(String itemName, int damage, String nbtHash) {
		this.itemName = itemName;
		this.damage = damage;
		this.nbtHash = nbtHash;
	}

	@Override
	public String toString() {
		return "TradeDescriptor [itemName=" + itemName + ", damage=" + damage + ", nbtHash=" + nbtHash + "]";
	}

}
