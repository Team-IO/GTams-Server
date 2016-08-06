package net.teamio.gtams.server.info;

public class TradeDescriptor {

	public String itemName;
	public int damage;
	public String nbtHash;

	//TODO: serialized NBT instead of hash? (use hash to identify, but send nbt data as well)

	public TradeDescriptor() {
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
		if(nbtHash.isEmpty()) {
			return itemName + "@" + Integer.toString(damage);
		}
		return itemName + "@" + Integer.toString(damage) + " +{NBT}";
	}

}
