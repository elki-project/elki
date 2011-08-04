package experimentalcode.erich.gearth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

public class MapWebServer {
  protected final static Logging logger = Logging.getLogger(MapWebServer.class);

  public final static String PATH_JSON = "/json/";

  private HttpServer server;

  private Database db;

  private HierarchicalResult result;

  public MapWebServer(int port, Database db, HierarchicalResult result) {
    super();
    this.db = db;
    this.result = result;

    try {
      InetSocketAddress addr = new InetSocketAddress(port);
      server = HttpServer.create(addr, 0);

      server.createContext(PATH_JSON, new JSONHttpHandler());
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

  /**
   * Parse a string into a DBID.
   * 
   * @param query Query string
   * @return DBID
   */
  protected DBID stringToDBID(String query) {
    return DBIDUtil.importInteger(Integer.valueOf(query));
  }

  protected void bundleToJSON(JSONBuffer re, DBID id) {
    SingleObjectBundle bundle = db.getBundle(id);
    if(bundle != null) {
      for(int j = 0; j < bundle.metaLength(); j++) {
        final Object data = bundle.data(j);
        // TODO: refactor to JSONFormatters!
        if(data instanceof NumberVector) {
          NumberVector<?, ?> v = (NumberVector<?, ?>) data;
          re.appendKeyArray(bundle.meta(j));
          for(int i = 0; i < v.getDimensionality(); i++) {
            re.append(v.doubleValue(i + 1));
          }
          re.closeArray();
        }
        else if(data instanceof PolygonsObject) {
          re.appendKeyArray(bundle.meta(j));
          for(Polygon p : ((PolygonsObject) data).getPolygons()) {
            re.startArray();
            for(int i = 0; i < p.size(); i++) {
              Vector point = p.get(i);
              re.append(point.getArrayRef());
            }
            re.closeArray();
          }
          re.closeArray();
        }
        else {
          re.appendKeyValue(bundle.meta(j), data);
        }
        if(logger.isDebuggingFiner()) {
          re.appendNewline();
        }
      }
    }
    else {
      re.appendKeyValue("error", "Object not found.");
    }
  }

  protected void resultToJSON(JSONBuffer re, String name) {
    if(result == null) {
      re.appendKeyValue("error", "no results available");
      return;
    }
    // Find requested result
    String[] parts = name.split("/");
    ResultHierarchy hier = result.getHierarchy();
    Result cur = result;
    int partpos = 0;
    for(; partpos < parts.length; partpos++) {
      // FIXME: handle name collisions. E.g. type_123?
      boolean found = false;
      for(Result child : hier.getChildren(cur)) {
        // logger.debug("Testing result: " + child.getShortName() + " <-> " +
        // parts[partpos]);
        if(child.getLongName().equals(parts[partpos]) || child.getShortName().equals(parts[partpos])) {
          cur = child;
          found = true;
          break;
        }
      }
      if(!found) {
        break;
      }
    }
    if(cur == null) {
      re.appendKeyValue("error", "result not found.");
      return;
    }
    // logger.debug(FormatUtil.format(parts, ",") + " " + partpos + " " + cur);
    // Result structure discovery:
    if(parts.length == partpos + 1) {
      if("children".equals(parts[partpos])) {
        re.appendKeyArray("children");
        Iterator<Result> iter = hier.getChildren(cur).iterator();
        while(iter.hasNext()) {
          Result child = iter.next();
          re.appendString(child.getShortName());
        }
        re.closeArray();
        return;
      }
    }
    // Database object access
    if(cur instanceof Database) {
      if(parts.length == partpos + 1) {
        DBID id = stringToDBID(parts[partpos]);
        if(id != null) {
          bundleToJSON(re, id);
          return;
        }
        else {
          re.appendKeyValue("error", "Object not found");
          return;
        }
      }
    }
    // Relation object access
    if(cur instanceof Relation) {
      if(parts.length == partpos + 1) {
        Relation<?> rel = (Relation<?>) cur;
        DBID id = stringToDBID(parts[partpos]);
        if(id != null) {
          Object data = rel.get(id);
          re.appendKeyValue("data", data);
        }
        else {
          re.appendKeyValue("error", "Object not found");
          return;
        }
      }
    }
    if(cur instanceof NeighborSetPredicate) {
      if(parts.length == partpos + 1) {
        NeighborSetPredicate pred = (NeighborSetPredicate) cur;
        DBID id = stringToDBID(parts[partpos]);
        if(id != null) {
          DBIDs neighbors = pred.getNeighborDBIDs(id);
          re.appendKeyValue("DBID", id);
          re.appendKeyArray("neighbors");
          for(DBID nid : neighbors) {
            re.appendString(nid.toString());
          }
          re.closeArray();
          return;
        }
        else {
          re.appendKeyValue("error", "Object not found");
          return;
        }
      }
    }
    if(cur instanceof OutlierResult) {
      if(parts.length >= partpos + 1) {
        if("table".equals(parts[partpos])) {
          int offset = 0;
          int pagesize = 100;

          if(parts.length >= partpos + 2) {
            offset = Integer.valueOf(parts[partpos + 1]);
          }
          if(parts.length >= partpos + 3) {
            pagesize = Integer.valueOf(parts[partpos + 2]);
          }

          re.appendKeyArray("scores");
          OutlierResult or = (OutlierResult) cur;
          Relation<Double> scores = or.getScores();
          Iterator<DBID> iter = or.getOrdering().iter(scores.getDBIDs()).iterator();
          for(int i = 0; i < offset && iter.hasNext(); i++) {
            iter.next();
          }
          for(int i = 0; i < pagesize && iter.hasNext(); i++) {
            DBID id = iter.next();
            re.startHash();
            bundleToJSON(re, id);
            final Double val = scores.get(id);
            if(val != null) {
              re.appendKeyValue("score", val);
            }
            re.closeHash();
          }
          re.closeArray();
          return;
        }
      }
    }
    re.appendKeyValue("error", "unknown query");
  }

  private class JSONHttpHandler implements HttpHandler {
    public JSONHttpHandler() {
      // Nothing to do.
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String requestMethod = exchange.getRequestMethod();
      if(!requestMethod.equalsIgnoreCase("GET")) {
        return;
      }
      String path = exchange.getRequestURI().getPath();
      logger.debug("Request for " + path);
      if(path.startsWith(PATH_JSON)) {
        path = path.substring(PATH_JSON.length());
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
        // if(logger.isDebuggingFinest() && callback != null) {
        //  logger.debugFinest("Callback parameter: " + callback);
        // }
      }

      // Prepare JSON response.
      StringBuffer response = new StringBuffer();
      if(callback != null) {
        response.append(callback);
        response.append("(");
      }
      JSONBuffer jsonbuf = new JSONBuffer(response);
      try {
        jsonbuf.startHash();
        resultToJSON(jsonbuf, path);
        jsonbuf.closeHash();
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
        response.append(")");
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