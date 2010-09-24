package experimentalcode.erich;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;

/**
 * A pseudo result handler that will just show the structure of the result
 * object.
 * 
 * TODO: transform this into an evaluator, then visualize, too?
 * 
 * @author Erich Schubert
 */
public class LogResultStructure implements ResultHandler<DatabaseObject, Result> {
  Logging logger = Logging.getLogger(LogResultStructure.class);

  @Override
  public void processResult(@SuppressWarnings("unused") Database<DatabaseObject> db, Result result) {
    StringBuffer buf = new StringBuffer();
    recursiveLogResult(buf, result, 0);
    logger.verbose(buf.toString());
  }

  /**
   * Recursively walk through the result tree.
   * 
   * @param buf Output buffer
   * @param result Current result
   * @param depth Depth
   */
  private void recursiveLogResult(StringBuffer buf, Result result, int depth) {
    if (depth > 50) {
      logger.warning("Probably infinitely nested results, aborting!");
      return;
    }
    for(int i = 0; i < depth; i++) {
      buf.append(" ");
    }
    buf.append(result.getClass().getSimpleName()).append(": ").append(result.getName()).append("\n");
    if(result instanceof MultiResult) {
      MultiResult mr = (MultiResult) result;
      for(Result r : mr.getResults()) {
        recursiveLogResult(buf, r, depth + 1);
      }
    }
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<DatabaseObject> normalization) {
    // Ignore
  }
}
