package experimentalcode.erich.gearth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

public class MapWebServer {
  protected final static Logging logger = Logging.getLogger(MapWebServer.class);

  public final static String PATH_JSONP = "/json/objects/";

  private HttpServer server;

  protected Map<String, PolygonsObject> polymap;

  public MapWebServer(int port, Map<String, PolygonsObject> polymap) {
    super();
    this.polymap = polymap;

    try {
      InetSocketAddress addr = new InetSocketAddress(port);
      server = HttpServer.create(addr, 0);

      server.createContext(PATH_JSONP, new JSONNeighborhoodHandler());
      server.setExecutor(Executors.newCachedThreadPool());
      server.start();

      logger.verbose("Webserver started on port " + port + ".");
    }
    catch(IOException e) {
      throw new AbortException("Could not start mini web server.", e);
    }
  }

  /**
   * Stop the web server.
   */
  public void stop() {
    server.stop(0);
  }

  private class JSONNeighborhoodHandler implements HttpHandler {
    public JSONNeighborhoodHandler() {
      // TODO Auto-generated constructor stub
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String requestMethod = exchange.getRequestMethod();
      if(!requestMethod.equalsIgnoreCase("GET")) {
        return;
      }
      String path = exchange.getRequestURI().getPath();
      logger.debug("Request for " + path);
      if(path.startsWith(PATH_JSONP)) {
        path = path.substring(PATH_JSONP.length());
      }
      else {
        logger.warning("Unexpected path in request handler: " + path);
        throw new AbortException("Unexpected path: " + path);
      }

      // Get JSON-with-padding callback name.
      String callback = null;
      {
        String query = exchange.getRequestURI().getQuery();
        if(query != null) {
          String[] frags = query.split("&");
          for(String frag : frags) {
            if(frag.startsWith("jsonp=")) {
              callback = URLDecoder.decode(frag.substring("jsonp=".length()), "UTF-8");
            }
            if(frag.startsWith("callback=")) {
              callback = URLDecoder.decode(frag.substring("callback=".length()), "UTF-8");
            }
          }
        }
        if(logger.isDebuggingFinest() && callback != null) {
          logger.debugFinest("Callback parameter: " + callback);
        }
      }

      // Prepare JSON response.
      StringBuffer response = new StringBuffer();
      if(callback != null) {
        response.append(callback);
        response.append("({");
      }
      else {
        response.append("{");
      }
      try {
        buildResponse(response, path);
      }
      catch(Exception e) {
        logger.warning("Exception occurred in embedded web server:", e);
        throw (new IOException(e));
      }
      // wrap up
      if(callback != null) {
        response.append("})");
      }
      else {
        response.append("}");
      }
      // Send
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/javascript");
      exchange.sendResponseHeaders(200, response.length());
      OutputStream responseBody = exchange.getResponseBody();
      responseBody.write(response.toString().getBytes());
      responseBody.close();
    }

    public void buildResponse(StringBuffer responseBody, String name) {
      // Add actual response.
      PolygonsObject polys = polymap.get(name);
      if(polys != null) {
        // logger.debugFinest("Polygon is: "+polys.toString());
        responseBody.append("\"polys\":[");
        Iterator<Polygon> polyit = polys.getPolygons().iterator();
        while(polyit.hasNext()) {
          Polygon poly = polyit.next();
          responseBody.append("[");
          Iterator<double[]> iter = poly.iterator();
          while(iter.hasNext()) {
            double[] data = iter.next();
            responseBody.append("[");
            for(int i = 0; i < data.length; i++) {
              if(i > 0) {
                responseBody.append(",");
              }
              responseBody.append(Double.toString(data[i]));
            }
            responseBody.append("]");
            if(iter.hasNext()) {
              responseBody.append(",");
            }
          }
          responseBody.append("]");
          if(polyit.hasNext()) {
            responseBody.append(",");
          }
        }
        responseBody.append("],");
      }

      responseBody.append("\"query\":\"" + jsonEscapeString(name) + "\"");
    }
    
    private String jsonEscapeString(String orig) {
      return orig.replace("\\", "\\\\").replace("\"", "\\\"");
    }
  }
}