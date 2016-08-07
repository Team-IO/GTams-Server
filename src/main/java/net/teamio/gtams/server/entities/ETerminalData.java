package net.teamio.gtams.server.entities;

import java.util.UUID;

public class ETerminalData {

	public UUID id;
	public UUID owner;
	public boolean online;

	public ETerminalData(UUID id, UUID owner, boolean online) {
		this.id = id;
		this.owner = owner;
		this.online = online;
	}

}
