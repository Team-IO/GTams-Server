package net.teamio.gtams.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.teamio.gtams.server.info.Goods;
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

		Set<Trade> completedBuy = new HashSet<>();
		Set<Trade> completedSell = new HashSet<>();

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
						completedBuy.add(b);
						completedSell.add(s);
						sell.remove(s);
						store.deleteTrade(s);
						store.deleteTrade(b);
						break;
					}
				}
			}
		}
	}

	private boolean amountSufficient(Trade buy, Trade sell) {
		if(sell.allowPartialFulfillment) {
			// Seller allows partial fulfillment, amount can be smaller than sales volume
			return buy.allowPartialFulfillment || sell.amount >= buy.amount;
		}
		// Seller does not allow partial fulfullment, amount has to be >= sales volume
		return buy.amount == sell.amount || (buy.allowPartialFulfillment && buy.amount > sell.amount);
	}

	private boolean makeTransaction(Trade buy, Trade sell, int amount) {
		Terminal customerTerminal = store.getTerminal(buy.terminalId);
		Terminal vendorTerminal = store.getTerminal(sell.terminalId);
		int price = Math.min(buy.price, sell.price);
		int cost = price * amount;
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

								store.saveGoods(goods);
								store.saveGoods(delivery);
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
