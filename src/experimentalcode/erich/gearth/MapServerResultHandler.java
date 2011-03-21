package experimentalcode.erich.gearth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DataQuery;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Handle results by serving them via a web server to mapping applications.
 * 
 * @author Erich Schubert
 */
public class MapServerResultHandler implements ResultHandler<DatabaseObject, Result> {
  /**
   * Polygon input file parameter
   * <p>
   * Key: {@code -mapserv.polygonparser}
   * </p>
   */
  public static final OptionID POLYGONS_ID = OptionID.getOrCreateOptionID("mapserv.polygonparser", "Polygon file parser");

  /**
   * Parameter that specifies the name of the input file to be parsed.
   * <p>
   * Key: {@code -mapserver.polygonfile}
   * </p>
   */
  public static final OptionID POLYGONS_FILE_ID = OptionID.getOrCreateOptionID("mapserver.polygonfile", "File name containing the polygons.");

  /**
   * The parser instance.
   */
  private Parser<PolygonsObject> polygonParser;

  private File inputFile;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MapServerResultHandler(Parameterization config) {
    super();
    config = config.descend(this);
    // polygon data source

    final ObjectParameter<Parser<PolygonsObject>> pparse_param = new ObjectParameter<Parser<PolygonsObject>>(POLYGONS_ID, Parser.class, SimplePolygonParser.class);
    polygonParser = (config.grab(pparse_param)) ? pparse_param.instantiateClass(config) : null;

    final FileParameter inputParam = new FileParameter(POLYGONS_FILE_ID, FileParameter.FileType.INPUT_FILE);
    inputFile = (config.grab(inputParam)) ? inputParam.getValue() : null;

  }

  @Override
  public void processResult(Database<DatabaseObject> db, Result result) {
    // Build a map for the main database, using external IDs
    Map<String, DBID> lblmap = new HashMap<String, DBID>(db.size() * 2);
    {
      DataQuery<String> olq = db.getObjectLabelQuery();
      DataQuery<String> eidq = db.getExternalIdQuery();
      for(DBID id : db) {
        if(olq != null) {
          String label = olq.get(id);
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
    // Build the polygon map
    Map<String, PolygonsObject> polymap = new HashMap<String, PolygonsObject>(db.size());
    {
      InputStream in = null;
      try {
        in = new FileInputStream(inputFile);
        in = FileUtil.tryGzipInput(in);
      }
      catch(IOException e) {
        throw new AbortException("Error loading polygon data file.");
      }
      ParsingResult<PolygonsObject> polys = polygonParser.parse(in);
      // Build reverse map
      for(Pair<PolygonsObject, List<String>> pair : polys.getObjectAndLabelList()) {
        for(String lbl : pair.getSecond()) {
          polymap.put(lbl, pair.first);
        }
      }
    }

    // FIXME: Make port configurable.
    MapWebServer serv = new MapWebServer(8080, lblmap, polymap, db);

    // TODO: stop somehow. UI with stop button?
    // watch for restarts due to result changes.
  }

  @SuppressWarnings("unused")
  @Override
  public void setNormalization(Normalization<DatabaseObject> normalization) {
    // ignore.
  }
}
