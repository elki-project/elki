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

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
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
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
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
      Relation<ExternalID> eidq = null;
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
          ExternalID eid = eidq.get(id);
          if(eid != null) {
            lblmap.put(eid.toString(), id);
          }
        }
      }
    }

    try {
      InetSocketAddress addr = new InetSocketAddress(port);
      server = HttpServer.create(addr, 0);

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
        re.appendString(bundle.meta(j)).appendRaw(":");
        final Object data = bundle.data(j);
        // TODO: refactor to JSONFormatters!
        if(data instanceof NumberVector) {
          NumberVector<?, ?> v = (NumberVector<?, ?>) data;
          re.appendRaw("[");
          for(int i = 0; i < v.getDimensionality(); i++) {
            if(i > 0) {
              re.appendRaw(",");
            }
            re.appendRaw(FormatUtil.format(v.doubleValue(i + 1)));
          }
          re.appendRaw("]");
        }
        else if(data instanceof PolygonsObject) {
          re.appendRaw("[");
          boolean first = true;
          for(Polygon p : ((PolygonsObject) data).getPolygons()) {
            if(first) {
              first = false;
            }
            else {
              re.appendRaw(",");
            }
            re.appendRaw("[");
            for(int i = 0; i < p.size(); i++) {
              if(i > 0) {
                re.appendRaw(",");
              }
              Vector point = p.get(i);
              re.appendRaw(point.toStringNoWhitespace());
            }
            re.appendRaw("]");
          }
          re.appendRaw("]");
        }
        else {
          re.appendString(data);
        }
        re.appendRaw(",");
        if(logger.isDebuggingFiner()) {
          re.appendRaw("\n");
        }
      }
    } else {
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
        // logger.debug("Testing result: " + child.getShortName() + " <-> " + parts[partpos]);
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
        re.appendString("children").appendRaw(":[");
        Iterator<Result> iter = hier.getChildren(cur).iterator();
        while(iter.hasNext()) {
          Result child = iter.next();
          re.appendString(child.getShortName());
          if(iter.hasNext()) {
            re.appendRaw(",");
          }
        }
        re.appendRaw("],");
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
          re.appendString("neighbors").appendRaw(":[");
          for(DBID nid : neighbors) {
            re.appendRaw(nid.toString()).appendRaw(",");
          }
          re.appendRaw("],");
          return;
        }
        else {
          re.appendKeyValue("error", "Object not found");
          return;
        }
      }
    }
    if(cur instanceof OutlierResult) {
      if(parts.length == partpos + 1) {
        if("table".equals(parts[partpos])) {
          int offset = 0;

          int pagesize = 50;
          re.appendString("scores").appendRaw(":[");
          OutlierResult or = (OutlierResult) cur;
          Relation<Double> scores = or.getScores();
          Iterator<DBID> iter = or.getOrdering().iter(scores.getDBIDs()).iterator();
          for(int i = 0; i < offset && iter.hasNext(); i++) {
            iter.next();
          }
          for(int i = 0; i < pagesize && iter.hasNext(); i++) {
            DBID id = iter.next();
            re.appendRaw("{");
            bundleToJSON(re, id);
            final Double val = scores.get(id);
            if(val != null) {
              re.appendKeyValue("score", val);
            }
            re.appendRaw("}");
            if(iter.hasNext()) {
              re.appendRaw(",");
            }
          }
          re.appendRaw("],");
          return;
        }
      }
    }
    re.appendKeyValue("error", "unknown query");
  }

  public static String jsonEscapeString(String orig) {
    return orig.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public class JSONBuffer {
    StringBuffer buffer;

    public JSONBuffer(StringBuffer buffer) {
      this.buffer = buffer;
    }

    public JSONBuffer appendString(Object cont) {
      final String str;
      if(cont instanceof String) {
        str = (String) cont;
      }
      else if(cont == null) {
        str = "null";
      }
      else {
        str = cont.toString();
      }
      buffer.append("\"").append(jsonEscapeString(str)).append("\"");
      return this;
    }

    public JSONBuffer appendKeyValue(Object key, Object val) {
      appendString(key);
      buffer.append(":");
      appendString(val);
      buffer.append(",");
      return this;
    }

    public JSONBuffer appendRaw(String chars) {
      buffer.append(chars);
      return this;
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
        resultToJSON(new JSONBuffer(response), path);
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