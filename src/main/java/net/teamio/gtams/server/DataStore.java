package net.teamio.gtams.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.teamio.gtams.server.info.Trade;
import net.teamio.gtams.server.info.TradeDescriptor;
import net.teamio.gtams.server.info.TradeInfo;
import net.teamio.gtams.server.storeentities.Terminal;


public class DataStore {

	private Map<UUID, Terminal> terminals;
	private Map<UUID, List<Trade>> terminalTrades;

	private Set<Trade> buyTrades;
	private Set<Trade> sellTrades;

	private Set<TradeDescriptor> activeDescriptors;
	private Set<TradeDescriptor> inactiveDescriptors;

	public DataStore() {
		terminals = new HashMap<>();
		activeDescriptors = new HashSet<>();
		inactiveDescriptors = new HashSet<>();
		terminalTrades = new HashMap<>();
	}

	public Terminal getTerminal(UUID id) {
		return terminals.get(id);
	}

	public void addTerminal(Terminal terminal) {
		if(terminals.containsKey(terminal.id)) {
			throw new DuplicateKeyException("Trying to add duplicate terminal id " + terminal.id);
		}
		terminals.put(terminal.id, terminal);
	}

	public void saveTerminal(Terminal terminal) {
		//TODO: Trigger DB update
	}

	public void deleteSavedTerminal(UUID id) {
		//TODO: delete form DB
	}

	public void saveDescriptor(TradeDescriptor descriptor) {
		//TODO: Trigger DB update
	}

	public void saveTrade(Trade trade) {
		//TODO: Trigger DB update
	}

	public void deleteSavedTrade(Trade trade) {
		//TODO: delete form DB
	}

	public void addTrade(Trade newTrade) {
		// Update descriptors
		if(activeDescriptors.add(newTrade.descriptor)) {
			if(inactiveDescriptors.remove(newTrade.descriptor)) {
				// New descriptor -> trigger save
				saveDescriptor(newTrade.descriptor);
			}
		}
		// Update trade info
		List<Trade> trades = terminalTrades.get(newTrade.terminalId);
		if(trades == null) {
			trades = new ArrayList<>();
			terminalTrades.put(newTrade.terminalId, trades);
		}
		trades.add(newTrade);
		if(newTrade.isBuy) {
			buyTrades.add(newTrade);
		} else {
			sellTrades.add(newTrade);
		}
		saveTrade(newTrade);
	}

	public List<Trade> getTradesForTerminal(UUID terminalId) {
		return terminalTrades.get(terminalId);
	}

	public void deleteTrade(Trade trade) {
		buyTrades.remove(trade);
		sellTrades.remove(trade);

		List<Trade> trades = terminalTrades.get(trade.terminalId);
		if(trades != null) {
			trades.remove(trade);
			if(trades.isEmpty()) {
				terminalTrades.remove(trade.terminalId);
			}
		}
		deleteSavedTrade(trade);
	}

	public TradeInfo getTradeInfo(TradeDescriptor ent) {

		//TODO: Actually store & fetch this data.

		TradeInfo info = new TradeInfo();
		Random rand = new Random();
		info.demand = rand.nextInt(30000);
		info.supply = rand.nextInt(30000);

		if(info.demand == 0) {
			info.supplyDemandFactor = Float.POSITIVE_INFINITY;
		} else {
			info.supplyDemandFactor = info.supply / (float)info.demand;
		}
		info.tradesLastPeriod = rand.nextInt(30000) + 100;
		info.volumeLastPeriod = rand.nextInt(30000) + 100;
		info.meanPrice = info.volumeLastPeriod / (float)info.tradesLastPeriod;

		return info;
	}

	public void setPlayerStatus(UUID id, boolean online) {
		// TODO Auto-generated method stub

	}

	public void deleteTerminal(UUID id) {
		// TODO Auto-generated method stub

		List<Trade> tradesForTerminal = getTradesForTerminal(id);

		buyTrades.removeAll(tradesForTerminal);
		sellTrades.removeAll(tradesForTerminal);

		for(Trade tr : tradesForTerminal) {
			deleteSavedTrade(tr);
		}

		terminalTrades.remove(id);
		terminals.remove(id);
		deleteSavedTerminal(id);
	}
}
