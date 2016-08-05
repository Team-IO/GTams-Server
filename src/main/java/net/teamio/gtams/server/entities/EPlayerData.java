package net.teamio.gtams.server.entities;

import java.util.UUID;

public class EPlayerData {

	public UUID id;
	public boolean online;

	/**
	 * @param id
	 * @param online
	 */
	public EPlayerData(UUID id, boolean online) {
		this.id = id;
		this.online = online;
	}

}
