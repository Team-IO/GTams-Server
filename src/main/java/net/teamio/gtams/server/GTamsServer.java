package net.teamio.gtams.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

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
import net.teamio.gtams.server.entities.ETerminalCreateTrade;
import net.teamio.gtams.server.entities.ETerminalData;
import net.teamio.gtams.server.entities.ETerminalOwner;
import net.teamio.gtams.server.info.Trade;
import net.teamio.gtams.server.info.TradeDescriptor;
import net.teamio.gtams.server.info.TradeInfo;
import net.teamio.gtams.server.info.TradeList;

public class GTamsServer {

	public static final int PORT = 20405;

	public static void main(String[] args) {
		new GTamsServer();
	}

	static Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

	public GTamsServer() {
		System.out.println("Setting up HTTP Server...");

		HttpProcessor processor = HttpProcessorBuilder.create().add(new ResponseDate())
				.add(new ResponseServer("GTams Server/1.0")).add(new ResponseContent()).add(new ResponseConnControl())
				.build();

		UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();

		HttpService httpService = new HttpService(processor, mapper);

		//TODO: clean up these endpoints!!
		mapper.register("/authenticate", new RHAuth());
		mapper.register("/terminal_status", new RHTerminalStatus());
		mapper.register("/terminal_owner", new RHTerminalOwner());
		mapper.register("/terminal_trades", new RHTerminalTrades());
		mapper.register("/terminal_newtrade", new RHTerminalNewTrade());
		mapper.register("/newterminal", new RHNewTerminal());
		mapper.register("/destroyterminal", new RHDestroyTerminal());
		mapper.register("/player_status", new RHPlayerStatus());
		mapper.register("/trade", new RHTradeInfo());

		ListenerThread liThread = new ListenerThread(httpService);
		System.out.println("Server started...");

		liThread.start();
	}

	private static class ListenerThread extends Thread {
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

	private static class WorkerThread extends Thread {
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

	private static class RHAuth implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			EAuthenticate ent = new EAuthenticate(UUID.randomUUID().toString());

			System.out.println("Authenticating Client with 'token' random UUID: " + ent.token);

			response.setStatusCode(HttpStatus.SC_CREATED);
			response.setEntity(new StringEntity(gson.toJson(ent)));
		}

	}

	private static class RHTerminalStatus implements HttpRequestHandler {

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

					//TODO: Process the status
				}
			}
		}

	}

	private static class RHTerminalTrades implements HttpRequestHandler {

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

					ArrayList<Trade> trades = new ArrayList<>();

					trades.add(new Trade(new TradeDescriptor("minecraft:stick", 0, "")));
					trades.add(new Trade(new TradeDescriptor("minecraft:potato", 0, "")));
					trades.add(new Trade(new TradeDescriptor("minecraft:enchanted_book", 0, "98765ab6785f8765ed7657865a7865765c876a58765")));
					trades.add(new Trade(new TradeDescriptor("minecraft:diamond_sword", 302, "")));
					trades.add(new Trade(new TradeDescriptor("minecraft:beef", 0, "")));

					TradeList tl = new TradeList(trades);

					response.setStatusCode(HttpStatus.SC_OK);
					response.setEntity(new StringEntity(gson.toJson(tl)));
				}
			}
		}

	}

	private static class RHTerminalNewTrade implements HttpRequestHandler {

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

					ArrayList<Trade> trades = new ArrayList<>();

					//TODO: actually store the trade
					trades.add(ent.trade);
					trades.add(new Trade(new TradeDescriptor("minecraft:stick", 0, "")));
					trades.add(new Trade(new TradeDescriptor("minecraft:potato", 0, "")));
					trades.add(new Trade(new TradeDescriptor("minecraft:enchanted_book", 0, "98765ab6785f8765ed7657865a7865765c876a58765")));
					trades.add(new Trade(new TradeDescriptor("minecraft:diamond_sword", 302, "")));
					trades.add(new Trade(new TradeDescriptor("minecraft:beef", 0, "")));

					TradeList tl = new TradeList(trades);

					response.setStatusCode(HttpStatus.SC_CREATED);
					response.setEntity(new StringEntity(gson.toJson(tl)));
				}
			}
		}

	}

	private static class RHTerminalOwner implements HttpRequestHandler {

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

					//TODO: Process the status
				}
			}
		}

	}

	private static class RHNewTerminal implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			ETerminalData entity = new ETerminalData(UUID.randomUUID(), true);

			System.out.println("Creating new terminalwith ID: " + entity.id);

			response.setStatusCode(HttpStatus.SC_CREATED);
			response.setEntity(new StringEntity(gson.toJson(entity)));
		}

	}

	private static class RHDestroyTerminal implements HttpRequestHandler {

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

					//TODO: Process the status
				}
			}
		}

	}

	private static class RHPlayerStatus implements HttpRequestHandler {

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

					//TODO: Process the status
				}
			}
		}

	}

	private static class RHTradeInfo implements HttpRequestHandler {

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
					Random rand = new Random();

					TradeInfo info = new TradeInfo();
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

					response.setStatusCode(HttpStatus.SC_CREATED);
					response.setEntity(new StringEntity(gson.toJson(info)));
				}
			}
		}

	}
}
