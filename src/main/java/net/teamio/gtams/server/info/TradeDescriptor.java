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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + damage;
		result = prime * result + ((itemName == null) ? 0 : itemName.hashCode());
		result = prime * result + ((nbtHash == null) ? 0 : nbtHash.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradeDescriptor other = (TradeDescriptor) obj;
		if (damage != other.damage)
			return false;
		if (itemName == null) {
			if (other.itemName != null)
				return false;
		} else if (!itemName.equals(other.itemName))
			return false;
		if (nbtHash == null) {
			if (other.nbtHash != null)
				return false;
		} else if (!nbtHash.equals(other.nbtHash))
			return false;
		return true;
	}

}
