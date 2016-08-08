package net.teamio.gtams.server.info;

import java.util.UUID;

public class Trade {

	public UUID terminalId;

	public long tradeId;

	public TradeDescriptor descriptor;

	public boolean isBuy;
	public int price;
	public int interval;
	public int stopAfter;
	public Mode mode = Mode.Once;
	public int amount = 1;
	public boolean allowPartialFulfillment = true;

	public long createdAt;
	public int completed = 0;
	public long dueNext = 0;

	public static class Comparator implements java.util.Comparator<Trade> {

		public static final Comparator INSTANCE = new Comparator();

		private Comparator() {
		}

		@Override
		public int compare(Trade o1, Trade o2) {
			if(o1.price == o2.price) {
				return Long.compare(o1.createdAt, o2.createdAt);
			}
			return Integer.compare(o1.price, o2.price);
		}

	}

	public Trade(TradeDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public Trade() {

	}

	public void markCompleted() {
		completed ++;
		if(mode == Mode.Recurring) {
			dueNext = System.currentTimeMillis() + interval * 1000;
		}
	}

	public boolean isDue() {
		if(mode == Mode.Once) {
			return completed == 0;
		} else if(mode == Mode.Recurring) {
			return stopAfter == 0 || completed < stopAfter && dueNext <= System.currentTimeMillis();
		} else if(mode == Mode.Infinite) {
			return true;
		} else {
			// Should not happen
			System.out.println("Trade with unknown mode!! (" + mode + ')');
			return false;
		}
	}

	@Override
	public String toString() {
		return (isBuy ? "Buy " : "Sell ") + amount + (allowPartialFulfillment ? "(P)" : "") + "x" + descriptor + " for " + price + " " + (mode == Mode.Recurring ? " every " + interval + " seconds " + stopAfter + " times." : mode);
	}

	public String toDisplayString() {
		String buyOrSell = isBuy ? "Buy" : "Sell";
		String partial = allowPartialFulfillment ? "(P)" : "";
		String templateA = "%s Amount: %d %s Price: %d %s";
		String templateB = "%s Amount: %d %s Price: %d Every %d seconds.";
		String templateC = "%s Amount: %d %s Price: %d Every %d seconds, %d times.";

		if(mode == Mode.Recurring) {
			if(stopAfter > 0) {
				return String.format(templateB, buyOrSell, amount, partial, price, interval);
			}
			return String.format(templateC, buyOrSell, amount, partial, price, interval, stopAfter);
		}
		return String.format(templateA, buyOrSell, amount, partial, price, mode);
	}
}
