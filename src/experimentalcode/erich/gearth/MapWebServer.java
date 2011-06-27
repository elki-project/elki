package experimentalcode.erich.gearth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

public class MapWebServer {
  protected final static Logging logger = Logging.getLogger(MapWebServer.class);

  public final static String PATH_JSONP_OBJECTS = "/json/objects/";

  public final static String PATH_JSONP_RESULTS = "/json/results/";

  private HttpServer server;

  private Map<String, DBID> lblmap;

  private Database db;

  private HierarchicalResult result;

  public MapWebServer(int port, Database db, HierarchicalResult result) {
    super();
    this.db = db;
    this.result = result;

    // Build a map for the main database, using external IDs
    {
      Relation<?> olq = null;
      try {
        olq = db.getRelation(TypeUtil.GUESSED_LABEL);
      }
      catch(NoSupportedDataTypeException e) {
        // pass
      }
      Relation<String> eidq = null;
      try {
        eidq = db.getRelation(TypeUtil.EXTERNALID);
      }
      catch(NoSupportedDataTypeException e) {
        // pass
      }
      int size = ((olq != null) ? olq.size() : 0) + ((eidq != null) ? eidq.size() : 0);
      lblmap = new HashMap<String, DBID>(size);
      for(DBID id : olq.iterDBIDs()) {
        if(olq != null) {
          String label = olq.get(id).toString();
          if(label != null) {
            lblmap.put(label, id);
          }
        }
        if(eidq != null) {
          String eid = eidq.get(id);
          if(eid != null) {
            lblmap.put(eid, id);
          }
        }
      }
    }

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

  protected void objectToJSON(StringBuffer re, String query) {
    DBID id = lblmap.get(query);
    if(id != null) {
      bundleToJSON(re, id);
    }

    re.append("\"query\":\"" + jsonEscapeString(query) + "\"");
  }

  protected void bundleToJSON(StringBuffer re, DBID id) {
    SingleObjectBundle bundle = db.getBundle(id);
    for(int j = 0; j < bundle.metaLength(); j++) {
      re.append("\"" + jsonEscapeString(bundle.meta(j).toString()) + "\":\"" + jsonEscapeString(bundle.data(j).toString()) + "\",");
    }
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
    for(int i = 0; i < parts.length - 1; i++) {
      // TODO: handle name collisions. E.g. type_123?
      boolean found = false;
      for(Result child : hier.getChildren(cur)) {
        logger.debug("Testing result: " + child.getShortName() + " <-> " + parts[i]);
        if(child.getShortName().equals(parts[i])) {
          cur = child;
          found = true;
          break;
        }
      }
      if(!found) {
        cur = null;
        break;
      }
    }
    if(cur == null) {
      re.append("\"error\":\"result not found.\",");
      return;
    }
    if(parts.length >= 1) {
      if("children".equals(parts[parts.length - 1])) {
        re.append("\"children\":[");
        Iterator<Result> iter = hier.getChildren(cur).iterator();
        while(iter.hasNext()) {
          Result child = iter.next();
          re.append("\"").append(child.getShortName()).append("\"");
          if(iter.hasNext()) {
            re.append(",");
          }
        }
        re.append("],");
        return;
      }
      if(cur instanceof Database) {
        // TODO: list functions?
        objectToJSON(re, parts[parts.length - 1]);
        return;
      }
    }
    if(cur instanceof OutlierResult) {
      re.append("\"scores\":[");
      OutlierResult or = (OutlierResult) cur;
      AnnotationResult<Double> scores = or.getScores();
      Iterator<DBID> iter = or.getOrdering().iter(db.getDBIDs()).iterator();
      while(iter.hasNext()) {
        DBID id = iter.next();
        re.append("{");
        re.append("\"dbid\":\"").append(id.toString()).append("\",");
        bundleToJSON(re, id);
        final Double val = scores.getValueFor(id);
        if(val != null) {
          re.append("\"score\":\"").append(val).append("\"");
        }
        re.append("}");
        if(iter.hasNext()) {
          re.append(",");
        }
      }
      re.append("],");
      return;
    }
    re.append("\"error\":\"unknown id\",");
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
        logger.exception("Exception occurred in embedded web server:", e);
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
        logger.exception("Exception occurred in embedded web server:", e);
        throw (new IOException(e));
      }
      catch(Throwable e) {
        logger.exception("Exception occurred in embedded web server:", e);
        throw (new IOException(e));
      }
      // wrap up
      if(callback != null) {
        response.append("})");
      }
      else {
        response.append("}");
      }
      byte[] rbuf = response.toString().getBytes("UTF-8");
      // Send
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/javascript");
      exchange.sendResponseHeaders(200, rbuf.length);
      OutputStream responseBody = exchange.getResponseBody();
      responseBody.write(rbuf);
      responseBody.close();
    }
  }
}