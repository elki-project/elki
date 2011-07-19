package experimentalcode.erich.gearth;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Handle results by serving them via a web server to mapping applications.
 * 
 * @author Erich Schubert
 */
public class MapServerResultHandler implements ResultHandler {
  /**
   * Constructor.
   */
  public MapServerResultHandler() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // FIXME: Make port configurable.
    HierarchicalResult hresult = (newResult instanceof HierarchicalResult) ? ((HierarchicalResult) newResult) : null;
    Database db = ResultUtil.findDatabase(baseResult);
    MapWebServer serv = new MapWebServer(8080, db, hresult);

    // TODO: stop somehow. UI with stop button?
    // watch for restarts due to result changes.
  }
}