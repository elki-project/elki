package de.lmu.ifi.dbs.elki.evaluation.index;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Simple index analytics, which includes the toString() dump of index
 * information.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has IndexMetaResult oneway - - «create»
 * 
 * @param <O> Database object type
 */
public class IndexStatistics<O extends DatabaseObject> implements Evaluator<O> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public IndexStatistics(Parameterization config) {
    super();
    config = config.descend(this);
  }

  @Override
  public void processResult(Database<O> db, HierarchicalResult result) {
    Collection<String> header = null;
    final ArrayList<TreeIndex<O, ?, ?>> indexes = ResultUtil.filterResults(result, TreeIndex.class);
    for(TreeIndex<O, ?, ?> index : indexes) {
      header = new java.util.Vector<String>();
      header.add(index.toString());
    }
    Collection<Pair<String, String>> col = new java.util.Vector<Pair<String, String>>();
    IndexMetaResult analysis = new IndexMetaResult(col, header);
    ResultUtil.addChildResult(db, analysis);
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<O> normalization) {
    // unused
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
