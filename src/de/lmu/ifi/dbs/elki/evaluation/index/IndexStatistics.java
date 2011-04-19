package de.lmu.ifi.dbs.elki.evaluation.index;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Simple index analytics, which includes the toString() dump of index
 * information.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has IndexMetaResult oneway - - «create»
 */
public class IndexStatistics implements Evaluator {
  /**
   * Constructor.
   */
  public IndexStatistics() {
    super();
  }

  @Override
  public void processResult(Database db, Result result) {
    Collection<String> header = null;
    final ArrayList<TreeIndex<?, ?, ?>> indexes = ResultUtil.filterResults(result, TreeIndex.class);
    if (indexes == null || indexes.size() <= 0) {
      return;
    }
    for(TreeIndex<?, ?, ?> index : indexes) {
      header = new java.util.Vector<String>();
      header.add(index.toString());
    }
    Collection<Pair<String, String>> col = new java.util.Vector<Pair<String, String>>();
    IndexMetaResult analysis = new IndexMetaResult(col, header);
    db.getHierarchy().add(db, analysis);
  }

  /**
   * Result class.
   * 
   * @author Erich Schubert
   */
  public class IndexMetaResult extends CollectionResult<Pair<String, String>> {
    /**
     * Constructor.
     * 
     * @param col key value pairs
     * @param header header
     */
    public IndexMetaResult(Collection<Pair<String, String>> col, Collection<String> header) {
      super("Index Statistics", "index-meta", col, header);
    }
  }
}