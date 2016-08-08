package net.teamio.gtams.server;

import java.util.ArrayList;
import java.util.List;

import net.teamio.gtams.server.info.Goods;
import net.teamio.gtams.server.info.Mode;
import net.teamio.gtams.server.info.Trade;
import net.teamio.gtams.server.info.Transaction;
import net.teamio.gtams.server.storeentities.Player;
import net.teamio.gtams.server.storeentities.Terminal;

public class TradeMatcher {

	private DataStore store;

	public TradeMatcher(DataStore store) {
		this.store = store;
	}

	public void matchTrades() {
		List<Trade> buy = new ArrayList<>(store.getBuyTrades());
		buy.removeIf((Trade trade) -> !trade.isDue());
		List<Trade> sell = new ArrayList<>(store.getSellTrades());
		sell.removeIf((Trade trade) -> !trade.isDue());

		buy.sort(Trade.Comparator.INSTANCE);
		sell.sort(Trade.Comparator.INSTANCE);

		for(Trade b : buy) {
			for(Trade s : sell) {
				// Check same goods
				if(!b.descriptor.equals(s.descriptor)) {
					continue;
				}
				if(b.terminalId.equals(s.terminalId)) {
					continue;
				}
				// Buy price covers sell price
				if(b.price >= s.price && amountSufficient(b, s)) {
					if(makeTransaction(b, s, Math.min(b.amount, s.amount))) {
						sell.remove(s);
						if(s.mode == Mode.Once) {
							store.deleteTrade(s);
						}
						if(b.mode == Mode.Once) {
							store.deleteTrade(b);
						}
						break;
					}
				}
			}
		}
	}

	private static boolean amountSufficient(Trade buy, Trade sell) {
		if(sell.allowPartialFulfillment) {
			// Seller allows partial fulfillment, amount can be smaller than sales volume
			return buy.allowPartialFulfillment || sell.amount >= buy.amount;
		}
		// Seller does not allow partial fulfullment, amount has to be >= sales volume
		return buy.amount == sell.amount || (buy.allowPartialFulfillment && buy.amount > sell.amount);
	}

	private boolean makeTransaction(Trade buy, Trade sell, int amount) {
		Terminal customerTerminal = store.getTerminal(buy.terminalId, null);
		Terminal vendorTerminal = store.getTerminal(sell.terminalId, null);
		int price = Math.min(buy.price, sell.price);
		int cost = price * amount;

		if(customerTerminal.owner == null && vendorTerminal.owner == null) {
			// This might be problematic
			return false;
		}

		Player cus = store.getPlayer(customerTerminal.owner);
		Player ven = store.getPlayer(vendorTerminal.owner);
		//TODO: later for production:
//		if(cus == ven) {
//			return false;
//		}
		synchronized (ven) {
			synchronized (cus) {
				synchronized (vendorTerminal) {
					synchronized (customerTerminal) {
						// Check funds
						if(cus.funds >= cost) {

							// Check goods
							Goods goods = store.getGoods(sell.terminalId, sell.descriptor);
							if(goods.amount >= amount) {
								cus.funds -= cost;
								ven.funds += cost;

								Goods delivery = store.getGoods(buy.terminalId, buy.descriptor);

								int consume = Math.min(goods.amount, amount);
								goods.amount -= consume;
								delivery.amount += amount;

								store.saveGoods(buy.terminalId);
								store.saveGoods(sell.terminalId);
								store.savePlayer(cus);
								store.savePlayer(ven);
								store.addTransaction(new Transaction(vendorTerminal.owner, customerTerminal.owner, buy.descriptor, amount, price));
								return true;
							}
						}
						return false;
					}
				}
			}
		}

	}

}
