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

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DataQuery;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

public class MapWebServer {
  protected final static Logging logger = Logging.getLogger(MapWebServer.class);

  public final static String PATH_JSONP_OBJECTS = "/json/objects/";

  public final static String PATH_JSONP_RESULTS = "/json/results/";

  private HttpServer server;

  private Map<String, PolygonsObject> polymap;

  private Map<String, DBID> lblmap;

  private DataQuery<DatabaseObjectMetadata> metaq;

  private Database<? extends DatabaseObject> db;

  private HierarchicalResult result;

  public MapWebServer(int port, Map<String, DBID> lblmap, Map<String, PolygonsObject> polymap, Database<? extends DatabaseObject> db, HierarchicalResult result) {
    super();
    this.lblmap = lblmap;
    this.polymap = polymap;
    this.db = db;
    this.metaq = db.getMetadataQuery();
    this.result = result;

    try {
      InetSocketAddress addr = new InetSocketAddress(port);
      server = HttpServer.create(addr, 0);

      server.createContext(PATH_JSONP_OBJECTS, new JSONObjectHandler());
      server.createContext(PATH_JSONP_RESULTS, new JSONResultHandler());
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

  protected void objectToJSON(StringBuffer re, String name) {
    // Add actual response.
    PolygonsObject polys = polymap.get(name);
    if(polys != null) {
      // logger.debugFinest("Polygon is: "+polys.toString());
      re.append("\"polys\":[");
      Iterator<Polygon> polyit = polys.getPolygons().iterator();
      while(polyit.hasNext()) {
        Polygon poly = polyit.next();
        re.append("[");
        Iterator<double[]> iter = poly.iterator();
        while(iter.hasNext()) {
          double[] data = iter.next();
          re.append("[");
          for(int i = 0; i < data.length; i++) {
            if(i > 0) {
              re.append(",");
            }
            re.append(Double.toString(data[i]));
          }
          re.append("]");
          if(iter.hasNext()) {
            re.append(",");
          }
        }
        re.append("]");
        if(polyit.hasNext()) {
          re.append(",");
        }
      }
      re.append("],");
    }
    DBID id = lblmap.get(name);
    if(id != null) {
      // Add metadata.
      DatabaseObjectMetadata meta = metaq.get(id);
      if(meta.objectlabel != null) {
        re.append("\"label\":\"" + jsonEscapeString(meta.objectlabel) + "\",");
      }
      if(meta.classlabel != null) {
        re.append("\"class\":\"" + jsonEscapeString(meta.classlabel.toString()) + "\",");
      }
      // Add attributes
      DatabaseObject obj = db.get(id);
      if(obj != null) {
        re.append("\"data\":\"" + jsonEscapeString(obj.toString()) + "\",");
      }
    }

    re.append("\"query\":\"" + jsonEscapeString(name) + "\"");
  }

  protected void resultToJSON(StringBuffer re, String name) {
    if(result == null) {
      re.append("\"error\":\"no results available\",");
      return;
    }
    // Find requested result
    String[] parts = name.split("/");
    ResultHierarchy hier = result.getHierarchy();
    Result cur = result;
    for (int i = 0; i < parts.length; i++) {
      // TODO: handle name collisions. E.g. type_123?
      boolean found = false;
      for (Result child : hier.getChildren(cur)) {
        logger.debug("Testing result: "+child.getShortName()+" <-> "+parts[i]);
        if (child.getShortName().equals(parts[i])) {
          cur = child;
          found = true;
          break;
        }
      }
      if (!found) {
        cur = null;
        break;
      }
    }
    if (cur == null) {
      re.append("\"error\":\"result not found.\",");
      return;
    }
  }

  private static String jsonEscapeString(String orig) {
    return orig.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private class JSONObjectHandler implements HttpHandler {
    public JSONObjectHandler() {
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
      if(path.startsWith(PATH_JSONP_OBJECTS)) {
        path = path.substring(PATH_JSONP_OBJECTS.length());
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
        objectToJSON(response, path);
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
  }

  private class JSONResultHandler implements HttpHandler {
    public JSONResultHandler() {
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
      if(path.startsWith(PATH_JSONP_RESULTS)) {
        path = path.substring(PATH_JSONP_RESULTS.length());
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
        resultToJSON(response, path);
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
  }
}