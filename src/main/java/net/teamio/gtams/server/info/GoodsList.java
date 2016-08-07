package net.teamio.gtams.server.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GoodsList {
	public List<Goods> goods;

	public GoodsList() {
		goods = new ArrayList<>();
	}

	public GoodsList(List<Goods> goods) {
		this.goods = goods;
	}

	public GoodsList(Collection<Goods> goods) {
		this.goods = new ArrayList<>(goods);
	}
}
