package net.teamio.gtams.server.entities;

import java.util.UUID;

public class ETerminalCreateNew {

	public UUID owner;

	public ETerminalCreateNew() {
	}

	public ETerminalCreateNew(UUID owner) {
		this.owner = owner;
	}
}
