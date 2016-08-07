package net.teamio.gtams.server.info;

import java.util.ArrayList;
import java.util.Collection;

public class GoodsList {
	public ArrayList<Goods> goods;

	public GoodsList() {
		goods = new ArrayList<>();
	}

	public GoodsList(ArrayList<Goods> list) {
		this.goods = list;
		if (goods == null) {
			goods = new ArrayList<>();
		}
	}

	public GoodsList(Collection<Goods> list) {
		if (list == null) {
			this.goods = new ArrayList<>();
		} else {
			this.goods = new ArrayList<>(list);
		}
	}
}
