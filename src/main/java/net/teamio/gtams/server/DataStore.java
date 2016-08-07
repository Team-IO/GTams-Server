package net.teamio.gtams.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.teamio.gtams.server.info.Goods;
import net.teamio.gtams.server.info.GoodsList;
import net.teamio.gtams.server.info.Trade;
import net.teamio.gtams.server.info.TradeDescriptor;
import net.teamio.gtams.server.info.TradeInfo;
import net.teamio.gtams.server.info.Transaction;
import net.teamio.gtams.server.storeentities.Player;
import net.teamio.gtams.server.storeentities.Terminal;


public class DataStore {

	private static final int DEFAULT_FUNDS = 50;

	private final Map<UUID, Player> players;
	private final Map<UUID, Terminal> terminals;
	private final Map<UUID, Map<Long, Trade>> terminalTrades;
	private final Map<UUID, Map<TradeDescriptor, Goods>> terminalGoods;

	private final Set<Trade> buyTrades;
	private final Set<Trade> sellTrades;

	private final Set<TradeDescriptor> activeDescriptors;
	private final Set<TradeDescriptor> inactiveDescriptors;

	public DataStore() {
		terminals = new HashMap<>();
		activeDescriptors = new HashSet<>();
		inactiveDescriptors = new HashSet<>();
		terminalTrades = Collections.synchronizedMap(new HashMap<>());
		players = Collections.synchronizedMap(new HashMap<>());
		buyTrades = Collections.synchronizedSet(new HashSet<>());
		sellTrades = Collections.synchronizedSet(new HashSet<>());
		terminalGoods = Collections.synchronizedMap(new HashMap<>());
	}

	public Terminal getTerminal(UUID id) {
		Terminal term = terminals.get(id);
		if(term == null) {
			term = new Terminal();
			term.id = id;
			addTerminal(term);
		}
		return term;
	}

	public void addTerminal(Terminal terminal) {
		synchronized (terminals) {
			if(terminals.containsKey(terminal.id)) {
				throw new DuplicateKeyException("Trying to add duplicate terminal id " + terminal.id);
			}
			terminals.put(terminal.id, terminal);
		}
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

	public void savePlayer(Player player) {
		// TODO Update DB

	}

	public void saveGoods(Goods goods) {
		// TODO save to DB

	}

	private void deleteSavedGoods(Goods inStock) {
		// TODO Auto-generated method stub

	}

	public void addTransaction(Transaction transaction) {
		//transactions.add(transaction);
		//saveTransaction(transaction);
	}

	private long lastTradeID = 0;

	public void addTrade(Trade newTrade) {
		synchronized (activeDescriptors) {
			// Update descriptors
			if (activeDescriptors.add(newTrade.descriptor)) {
				if (inactiveDescriptors.remove(newTrade.descriptor)) {
					// New descriptor -> trigger save
					saveDescriptor(newTrade.descriptor);
				}
			}
		}
		synchronized (terminalTrades) {
			// Update trade info
			Map<Long, Trade> trades = terminalTrades.get(newTrade.terminalId);
			if (trades == null) {
				trades = new HashMap<>();
				terminalTrades.put(newTrade.terminalId, trades);
			}
			newTrade.tradeId = ++lastTradeID;
			trades.put(newTrade.tradeId, newTrade);
		}
		if (newTrade.isBuy) {
			buyTrades.add(newTrade);
		} else {
			sellTrades.add(newTrade);
		}
		saveTrade(newTrade);
	}

	public Collection<Trade> getTradesForTerminal(UUID terminalId) {
		Map<Long, Trade> trades = terminalTrades.get(terminalId);
		if(trades == null) {
			return Collections.emptyList();
		}
		return trades.values();
	}

	public void deleteTrade(Trade trade) {
		buyTrades.remove(trade);
		sellTrades.remove(trade);
		synchronized(terminalTrades) {
			Map<Long, Trade> trades = terminalTrades.get(trade.terminalId);
			if(trades != null) {
				trades.remove(trade.tradeId);
				if(trades.isEmpty()) {
					terminalTrades.remove(trade.terminalId);
				}
			}
		}
		deleteSavedTrade(trade);
	}

	public Trade getTrade(UUID terminalId, long tradeId) {
		synchronized(terminalTrades) {
			Map<Long, Trade> trades = terminalTrades.get(terminalId);
			if(trades != null) {
				return trades.get(tradeId);
			}
			return null;
		}
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
		Player player = getPlayer(id);
		player.online = online;
		savePlayer(player);
	}

	public void deleteTerminal(UUID id) {
		synchronized(terminals) {
			Collection<Trade> tradesForTerminal = getTradesForTerminal(id);

			buyTrades.removeAll(tradesForTerminal);
			sellTrades.removeAll(tradesForTerminal);

			for(Trade tr : tradesForTerminal) {
				deleteSavedTrade(tr);
			}
			//TODO: delete & return all related goods
			terminalTrades.remove(id);
			terminals.remove(id);
			deleteSavedTerminal(id);
		}
	}

	public Player getPlayer(UUID id) {
		Player player;
		synchronized(players) {
			player = players.get(id);
			if(player == null) {
				player = new Player();
				player.id = id;
				player.funds = DEFAULT_FUNDS;
				players.put(id, player);
				savePlayer(player);
			}
		}
		return player;
	}

	public void setTerminalStatus(UUID id, boolean online) {
		synchronized(terminals) {
			Terminal term = getTerminal(id);
			if(term == null) {
				//TODO: create new terminal?
			} else {
				term.online = online;
				saveTerminal(term);
			}
		}
	}

	public Set<Trade> getBuyTrades() {
		return buyTrades;
	}

	public Set<Trade> getSellTrades() {
		return sellTrades;
	}

	public Goods getGoods(UUID terminalID, TradeDescriptor what) {
		synchronized(terminalGoods) {
			Map<TradeDescriptor, Goods> goodsList = terminalGoods.get(terminalID);
			if(goodsList == null) {
				goodsList = new HashMap<>();
				terminalGoods.put(terminalID, goodsList);
			}
			Goods goods = goodsList.get(what);
			if(goods == null) {
				goods = new Goods();
				goods.what = what;
				goodsList.put(what, goods);
			}
			return goods;
		}
	}

	public GoodsList addGoods(UUID terminalID, List<Goods> request) {
		synchronized(terminalGoods) {
			if(request == null) {
				return new GoodsList();
			}
			Map<TradeDescriptor, Goods> goods = terminalGoods.get(terminalID);
			if(goods == null) {
				goods = new HashMap<>();
				terminalGoods.put(terminalID, goods);
			}
			for(Goods g : request) {
				Goods inStock = goods.get(g.what);
				if(inStock == null) {
					goods.put(g.what, g);
					saveGoods(g);
				} else {
					inStock.amount += g.amount;
					saveGoods(inStock);
				}
			}
			return new GoodsList(goods.values());
		}
	}

	public GoodsList removeGoods(UUID terminalID, List<Goods> request) {
		synchronized(terminalGoods) {
			if(request == null) {
				return new GoodsList();
			}
			Map<TradeDescriptor, Goods> goods = terminalGoods.get(terminalID);
			if(goods == null) {
				return new GoodsList();
			}
			GoodsList returnList = new GoodsList();
			for(Goods g : request) {
				Goods inStock = goods.get(g.what);
				if(inStock != null) {
					int deducted = Math.min(g.amount, inStock.amount);
					inStock.amount -= deducted;
					if(inStock.amount == 0) {
						goods.remove(inStock.what);
						deleteSavedGoods(inStock);
					}

					returnList.goods.add(new Goods(g.what, deducted));
				}
			}
			return returnList;
		}
	}

	public GoodsList getGoods(UUID terminalID) {
		synchronized(terminalGoods) {
			Map<TradeDescriptor, Goods> goods = terminalGoods.get(terminalID);
			if(goods == null) {
				return new GoodsList();
			}
			return new GoodsList(goods.values());
		}
	}
}
