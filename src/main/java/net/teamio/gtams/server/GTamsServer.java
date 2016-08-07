package net.teamio.gtams.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.teamio.gtams.server.entities.EAuthenticate;
import net.teamio.gtams.server.entities.EPlayerData;
import net.teamio.gtams.server.entities.ETerminalCreateNew;
import net.teamio.gtams.server.entities.ETerminalCreateTrade;
import net.teamio.gtams.server.entities.ETerminalData;
import net.teamio.gtams.server.entities.ETerminalDeleteTrade;
import net.teamio.gtams.server.entities.ETerminalGoodsData;
import net.teamio.gtams.server.entities.ETerminalOwner;
import net.teamio.gtams.server.info.Goods;
import net.teamio.gtams.server.info.GoodsList;
import net.teamio.gtams.server.info.Trade;
import net.teamio.gtams.server.info.TradeDescriptor;
import net.teamio.gtams.server.info.TradeInfo;
import net.teamio.gtams.server.info.TradeList;
import net.teamio.gtams.server.storeentities.Player;
import net.teamio.gtams.server.storeentities.Terminal;

public class GTamsServer {

	public static final int PORT = 20405;

	public static void main(String[] args) {
		new GTamsServer();
	}

	static Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

	private DataStore store;

	public GTamsServer() {
		System.out.println("Loading data store...");

		store = new DataStore();

		System.out.println("Setting up HTTP Server...");

		HttpProcessor processor = HttpProcessorBuilder.create().add(new ResponseDate())
				.add(new ResponseServer("GTams Server/1.0")).add(new ResponseContent()).add(new ResponseConnControl())
				.build();

		UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();

		HttpService httpService = new HttpService(processor, mapper);

		mapper.register("/authenticate", new RHAuth());

		mapper.register("/terminal/new", new RHNewTerminal());
		mapper.register("/terminal/destroy", new RHDestroyTerminal());
		mapper.register("/terminal/status", new RHTerminalStatus());
		mapper.register("/terminal/owner", new RHTerminalOwner());

		mapper.register("/terminal/trades", new RHTerminalTrades());
		mapper.register("/terminal/trades/add", new RHTerminalNewTrade());
		mapper.register("/terminal/trades/remove", new RHTerminalRemoveTrade());

		mapper.register("/terminal/goods", new RHTerminalGoodsGet());
		mapper.register("/terminal/goods/add", new RHTerminalGoodsAdd());
		mapper.register("/terminal/goods/remove", new RHTerminalGoodsRemove());

		mapper.register("/player", new RHPlayerInfo());
		mapper.register("/player/status", new RHPlayerStatus());
		mapper.register("/market/query", new RHMarketInfo());

		ListenerThread liThread = new ListenerThread(httpService);
		liThread.start();

		MatcherThread maThread = new MatcherThread();
		maThread.start();

		System.out.println("Server started...");
	}

	private class ListenerThread extends Thread {
		private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
		private ServerSocket socket;
		private final HttpService service;

		public ListenerThread(HttpService service) {
			super("GTams Listener Thread");
			this.service = service;
			this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
		}

		@Override
		public void run() {
			try {
				this.socket = new ServerSocket(PORT);
				System.out.println("Listening on port " + PORT);

				while (!Thread.interrupted()) {
					System.out.println("Waiting for connection...");
					Socket socket = this.socket.accept();
					HttpServerConnection conn = connFactory.createConnection(socket);

					Thread workerThread = new WorkerThread(service, conn);
					workerThread.setDaemon(true);
					workerThread.setName("GTams Worker Thread");
					workerThread.start();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class MatcherThread extends Thread {

		public MatcherThread() {
			super("GTams Matcher Thread");
		}

		@Override
		public void run() {
			TradeMatcher matcher = new TradeMatcher(store);
			while (!Thread.interrupted()) {
				try {
					matcher.matchTrades();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private class WorkerThread extends Thread {
		private final HttpServerConnection conn;
		private final HttpService service;

		public WorkerThread(HttpService service, HttpServerConnection conn) {
			this.service = service;
			this.conn = conn;
		}

		@Override
		public void run() {
			try {
				HttpContext context = new BasicHttpContext();
				while (!Thread.interrupted() && conn.isOpen()) {
					service.handleRequest(conn, context);
				}
			} catch(ConnectionClosedException e) {
				System.out.println("Client disconnected, terminating worker thread.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					conn.shutdown();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private class RHAuth implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			EAuthenticate ent = new EAuthenticate(UUID.randomUUID().toString());

			System.out.println("Authenticating Client with 'token' random UUID: " + ent.token);

			response.setStatusCode(HttpStatus.SC_CREATED);
			response.setEntity(new StringEntity(gson.toJson(ent)));
		}

	}

	private class RHTerminalStatus implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalData ent = gson.fromJson(json, ETerminalData.class);

					System.out.println("Terminal status of " + ent.id + " is " + (ent.online ? "ONLINE" : "OFFLINE"));

					store.setTerminalStatus(ent.id, ent.online);
				}
			}
		}

	}

	private class RHTerminalTrades implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalData ent = gson.fromJson(json, ETerminalData.class);

					System.out.println("Terminal " + ent.id + " requested current trades.");

					TradeList tl = new TradeList(store.getTradesForTerminal(ent.id));

					response.setStatusCode(HttpStatus.SC_OK);
					response.setEntity(new StringEntity(gson.toJson(tl)));
				}
			}
		}

	}

	private class RHTerminalNewTrade implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalCreateTrade ent = gson.fromJson(json, ETerminalCreateTrade.class);

					System.out.println("Terminal " + ent.id + " requested to create a new trade.");

					ent.trade.terminalId = ent.id;
					store.addTrade(ent.trade);

					TradeList tl = new TradeList(store.getTradesForTerminal(ent.id));

					response.setStatusCode(HttpStatus.SC_CREATED);
					response.setEntity(new StringEntity(gson.toJson(tl)));
				}
			}
		}

	}

	private class RHTerminalRemoveTrade implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalDeleteTrade ent = gson.fromJson(json, ETerminalDeleteTrade.class);

					System.out.println("Terminal " + ent.id + " requested to delete an existing trade.");

					Trade trade = store.getTrade(ent.id,ent.trade);
					store.deleteTrade(trade);

					TradeList tl = new TradeList(store.getTradesForTerminal(ent.id));

					response.setStatusCode(HttpStatus.SC_OK);
					response.setEntity(new StringEntity(gson.toJson(tl)));
				}
			}
		}

	}

	private class RHTerminalOwner implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalOwner ent = gson.fromJson(json, ETerminalOwner.class);

					System.out.println("Terminal " + ent.id + " is now owned by " + ent.owner);

					Terminal term = store.getTerminal(ent.id);
					term.owner = ent.owner;
					store.saveTerminal(term);
				}
			}
		}

	}

	private class RHNewTerminal implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalCreateNew ent = gson.fromJson(json, ETerminalCreateNew.class);
					System.out.println("Owner " + ent.owner + " requested new terminal.");

					ETerminalData responseEntity = new ETerminalData(UUID.randomUUID(), true);

					System.out.println("Creating new terminalwith ID: " + responseEntity.id);

					Terminal term = new Terminal();
					term.id = responseEntity.id;
					term.owner = ent.owner;

					store.addTerminal(term);

					response.setStatusCode(HttpStatus.SC_CREATED);
					response.setEntity(new StringEntity(gson.toJson(responseEntity)));
				}
			}


		}

	}

	private class RHTerminalGoodsGet implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalData ent = gson.fromJson(json, ETerminalData.class);
					System.out.println("Terminal " + ent.id + " requested goods list.");

					GoodsList gl = store.getGoods(ent.id);

					response.setStatusCode(HttpStatus.SC_CREATED);
					response.setEntity(new StringEntity(gson.toJson(gl)));
				}
			}
		}
	}

	private class RHTerminalGoodsAdd implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalGoodsData ent = gson.fromJson(json, ETerminalGoodsData.class);
					System.out.println("Terminal " + ent.id + " requested to add goods.");

					List<Goods> goods = ent.goods;

					for(Goods g : goods) {
						Goods inStore = store.getGoods(ent.id, g.what);
						inStore.amount += g.amount;
						store.saveGoods(inStore);
					}

					GoodsList gl = store.getGoods(ent.id);

					response.setStatusCode(HttpStatus.SC_CREATED);
					response.setEntity(new StringEntity(gson.toJson(gl)));
				}
			}
		}
	}

	private class RHTerminalGoodsRemove implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalGoodsData ent = gson.fromJson(json, ETerminalGoodsData.class);
					System.out.println("Terminal " + ent.id + " requested to remove goods.");

					GoodsList actuallyRemoved = store.removeGoods(ent.id, ent.goods);

					response.setStatusCode(HttpStatus.SC_OK);
					response.setEntity(new StringEntity(gson.toJson(actuallyRemoved)));
				}
			}
		}
	}

	private class RHDestroyTerminal implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					ETerminalData ent = gson.fromJson(json, ETerminalData.class);
					System.out.println("Destroying terminalwith ID: " + ent.id);

					store.deleteTerminal(ent.id);
				}
			}
		}

	}

	private class RHPlayerStatus implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					EPlayerData ent = gson.fromJson(json, EPlayerData.class);

					System.out.println("Player/Owner status of " + ent.id + " is " + (ent.online ? "ONLINE" : "OFFLINE"));

					store.setPlayerStatus(ent.id, ent.online);
				}
			}
		}

	}
	private class RHPlayerInfo implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					EPlayerData ent = gson.fromJson(json, EPlayerData.class);

					System.out.println("Player/Owner status of " + ent.id + " is " + (ent.online ? "ONLINE" : "OFFLINE"));

					Player player = store.getPlayer(ent.id);

					response.setStatusCode(HttpStatus.SC_OK);
					response.setEntity(new StringEntity(gson.toJson(player)));
				}
			}
		}

	}

	private class RHMarketInfo implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					String json = EntityUtils.toString(entity);
					TradeDescriptor ent = gson.fromJson(json, TradeDescriptor.class);

					System.out.println("Client requested trade info for " + ent);
					System.out.println(json);

					TradeInfo info = store.getTradeInfo(ent);


					response.setStatusCode(HttpStatus.SC_OK);
					response.setEntity(new StringEntity(gson.toJson(info)));
				}
			}
		}

	}
}
