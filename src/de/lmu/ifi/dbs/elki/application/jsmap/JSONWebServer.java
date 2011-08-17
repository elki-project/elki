package de.lmu.ifi.dbs.elki.application.jsmap;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * A simple web server to serve data base contents to a JavaScript client.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses JSONBuffer
 */
public class JSONWebServer implements HttpHandler {
  /**
   * Our logger
   */
  protected final static Logging logger = Logging.getLogger(JSONWebServer.class);

  /**
   * The base path we serve data from
   */
  public final static String PATH_JSON = "/json/";

  /**
   * Server instance
   */
  private HttpServer server;

  /**
   * The result tree we serve
   */
  private HierarchicalResult result;

  /**
   * The database we use for obtaining object bundles
   */
  private Database db;

  /**
   * Constructor.
   * 
   * @param port Port to listen on
   * @param result Result to serve
   */
  public JSONWebServer(int port, HierarchicalResult result) {
    super();
    this.result = result;
    assert (result != null) : "MapWebServer created with null result.";
    this.db = ResultUtil.findDatabase(result);

    try {
      InetSocketAddress addr = new InetSocketAddress(port);
      server = HttpServer.create(addr, 0);

      server.createContext(PATH_JSON, this);
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
  private DBID stringToDBID(String query) {
    return DBIDUtil.importInteger(Integer.valueOf(query));
  }

  /**
   * Serialize an object bundle to JSON.
   * 
   * @param re Buffer to serialize to
   * @param id Object ID
   */
  protected void bundleToJSON(JSONBuffer re, DBID id) {
    SingleObjectBundle bundle = db.getBundle(id);
    if(bundle != null) {
      for(int j = 0; j < bundle.metaLength(); j++) {
        final Object data = bundle.data(j);
        // TODO: refactor to JSONFormatters!
        // Format a NumberVector
        if(data instanceof NumberVector) {
          NumberVector<?, ?> v = (NumberVector<?, ?>) data;
          re.appendKeyArray(bundle.meta(j));
          for(int i = 0; i < v.getDimensionality(); i++) {
            re.append(v.doubleValue(i + 1));
          }
          re.closeArray();
        }
        // Format a Polygon
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
        // Default serialization as string
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

  /**
   * Serialize an arbitrary result into JSON.
   * 
   * @param re Buffer to serialize to
   * @param name Result requested
   */
  // TODO: refactor
  protected void resultToJSON(JSONBuffer re, String name) {
    ResultHierarchy hier = result.getHierarchy();
    // Find requested result
    String[] parts = name.split("/");
    Result cur = result;
    int partpos = 0;
    {
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
    }
    // logger.debug(FormatUtil.format(parts, ",") + " " + partpos + " " + cur);

    // Result structure discovery via "children" parameter.
    if(parts.length == partpos + 1) {
      if("children".equals(parts[partpos])) {
        re.appendKeyArray("children");
        Iterator<Result> iter = hier.getChildren(cur).iterator();
        while(iter.hasNext()) {
          Result child = iter.next();
          re.startHash();
          re.appendKeyValue("name", child.getShortName());
          re.appendKeyValue("type", child.getClass().getSimpleName());
          re.closeHash();
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

    // Neighbor access
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

    // Outlier Score access
    if(cur instanceof OutlierResult) {
      OutlierResult or = (OutlierResult) cur;
      if(parts.length >= partpos + 1) {
        if("table".equals(parts[partpos])) {
          // Handle paging
          int offset = 0;
          int pagesize = 500;

          if(parts.length >= partpos + 2) {
            offset = Integer.valueOf(parts[partpos + 1]);
          }
          if(parts.length >= partpos + 3) {
            pagesize = Integer.valueOf(parts[partpos + 2]);
          }
          re.appendKeyHash("paging");
          re.appendKeyValue("offset", offset);
          re.appendKeyValue("pagesize", pagesize);
          re.closeHash();
          if(logger.isDebuggingFiner()) {
            re.appendNewline();
          }

          // Serialize meta
          OutlierScoreMeta meta = or.getOutlierMeta();
          outlierMetaToJSON(re, meta);

          re.appendKeyArray("scores");
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

  /**
   * Serialize outlier metadata as JSON.
   * 
   * @param re Output buffer
   * @param meta Metadata
   */
  private void outlierMetaToJSON(JSONBuffer re, OutlierScoreMeta meta) {
    re.appendKeyHash("meta");
    re.appendKeyValue("min", meta.getActualMinimum());
    re.appendKeyValue("max", meta.getActualMaximum());
    re.appendKeyValue("tmin", meta.getTheoreticalMinimum());
    re.appendKeyValue("tmax", meta.getTheoreticalMaximum());
    re.appendKeyValue("base", meta.getTheoreticalBaseline());
    re.appendKeyValue("type", meta.getClass().getSimpleName());
    re.closeHash();
    if(logger.isDebuggingFiner()) {
      re.appendNewline();
    }
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if(!requestMethod.equalsIgnoreCase("GET")) {
      return;
    }
    String path = exchange.getRequestURI().getPath();
    // logger.debug("Request for " + path);
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
      // logger.debugFinest("Callback parameter: " + callback);
      // }
    }

    // Prepare JSON response.
    StringBuffer response = new StringBuffer();
    {
      if(callback != null) {
        response.append(callback);
        response.append("(");
      }

      // JSON serializer
      JSONBuffer jsonbuf = new JSONBuffer(response);
      try {
        jsonbuf.startHash();
        resultToJSON(jsonbuf, path);
        jsonbuf.closeHash();
      }
      catch(Throwable e) {
        logger.exception("Exception occurred in embedded web server:", e);
        throw (new IOException(e));
      }
      // wrap up
      if(callback != null) {
        response.append(")");
      }
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