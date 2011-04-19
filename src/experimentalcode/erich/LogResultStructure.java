package experimentalcode.erich;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;

/**
 * A pseudo result handler that will just show the structure of the result
 * object.
 * 
 * TODO: transform this into an evaluator, then visualize, too?
 * 
 * @author Erich Schubert
 */
public class LogResultStructure implements ResultHandler {
  Logging logger = Logging.getLogger(LogResultStructure.class);

  @Override
  public void processResult(@SuppressWarnings("unused") Database db, Result result) {
    if(logger.isVerbose()) {
      if(result instanceof HierarchicalResult) {
        Hierarchy<Result> hier = ((HierarchicalResult) result).getHierarchy();
        if(hier != null) {
          StringBuffer buf = new StringBuffer();
          recursiveLogResult(buf, hier, result, 0);
          logger.verbose(buf.toString());
        }
      }
    }
  }

  /**
   * Recursively walk through the result tree.
   * 
   * @param buf Output buffer
   * @param result Current result
   * @param depth Depth
   */
  private void recursiveLogResult(StringBuffer buf, Hierarchy<Result> hier, Result result, int depth) {
    if(result == null) {
      buf.append("null");
      logger.warning("null result!");
      return;
    }
    if(depth > 50) {
      logger.warning("Probably infinitely nested results, aborting!");
      return;
    }
    for(int i = 0; i < depth; i++) {
      buf.append(" ");
    }
    buf.append(result.getClass().getSimpleName()).append(": ").append(result.getLongName());
    buf.append(" (").append(result.getShortName()).append(")\n");
    if(hier.getChildren(result).size() > 0) {
      for(Result r : hier.getChildren(result)) {
        recursiveLogResult(buf, hier, r, depth + 1);
      }
    }
  }
}
