package misc.remote;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import jp.digitalmuseum.connector.RXTXConnector;
import jp.digitalmuseum.mr.Matereal;
import jp.digitalmuseum.mr.entity.RemoteStation;

/**
 * Control RemoteStation.
 *
 * @author Jun KATO
 */
public class RemoteStationServer implements HttpHandler {

	public static final int NUM_BACKLOG = 5;
	public static final String NAME_CONFIGFILE = "rss.dat";
	private HttpServer server;
	private DefaultFileHandler defaultFileHandler;
	private RemoteStationHandler rsHandler;

	public static void main(String[] args) throws IOException {
		new RemoteStationServer();
	}

	public RemoteStationServer() throws IOException {

		ParameterFilter filter = new ParameterFilter();

		defaultFileHandler = new DefaultFileHandler("public");

		RXTXConnector connector = new RXTXConnector("COM:/dev/tty.usbserial-00002480");
		connector.connect(115200,
				RXTXConnector.DATABITS_8,
				RXTXConnector.STOPBITS_1,
				RXTXConnector.PARITY_NONE);
		RemoteStation rs = new RemoteStation(connector);
		rsHandler = new RemoteStationHandler(rs);
		try {
			rsHandler.load(NAME_CONFIGFILE);
		} catch (ClassNotFoundException e) {
			//
		}

		server = HttpServer.create(new InetSocketAddress(8000), NUM_BACKLOG);
		server.createContext("/", defaultFileHandler);
		server.createContext("/remote", rsHandler).getFilters().add(filter);
		server.createContext("/system", this);
		server.setExecutor(null);
		server.start();
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		System.out.println(path);

		// Shutdown the server.
		if (path.equals("/system/off")) {
			rsHandler.save(NAME_CONFIGFILE);
			exchange.sendResponseHeaders(200, 0);
			exchange.close();
			Matereal.getInstance().dispose();
			this.server.stop(0);
			return;
		} else if (path.equals("/system/reload")) {
			rsHandler.save(NAME_CONFIGFILE);
			this.defaultFileHandler.loadFiles();
			exchange.sendResponseHeaders(200, 0);
			exchange.close();
			return;
		}

		exchange.sendResponseHeaders(404, 0);
		exchange.close();
	}
}
