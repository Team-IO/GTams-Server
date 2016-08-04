package net.teamio.gtams.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

public class GTamsServer {

	public static final int PORT = 20405;

	public static void main(String[] args) {
		new GTamsServer();
	}

	public GTamsServer() {
		System.out.println("Setting up HTTP Server...");

		HttpProcessor processor = HttpProcessorBuilder.create().add(new ResponseDate())
				.add(new ResponseServer("GTams Server/1.0")).add(new ResponseContent()).add(new ResponseConnControl())
				.build();

		UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();

		HttpService httpService = new HttpService(processor, mapper);

		mapper.register("/authenticate", new RHAuth());

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
			String randomUUID = UUID.randomUUID().toString();

			System.out.println("Authenticating Client with 'token' random UUID: " + randomUUID);

			response.setStatusCode(HttpStatus.SC_CREATED);
			response.setEntity(new StringEntity(randomUUID));
		}

	}

	private static class RequestHandler implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context)
				throws HttpException, IOException {

			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				if(entity == null) {
					//TODO whatever
				} else {
					//TODO: entity.getContent() => JSON
				}
			}
		}

	}
}
