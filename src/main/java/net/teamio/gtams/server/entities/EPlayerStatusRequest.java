package net.teamio.gtams.server.entities;

import java.util.UUID;

public class EPlayerStatusRequest {

	public UUID id;
	public String name;
	public boolean online;

	public EPlayerStatusRequest(UUID id, String name, boolean online) {
		this.id = id;
		this.name = name;
		this.online = online;
	}

}
