package de.lmu.ifi.dbs.elki.evaluation.index;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.IndexDatabase;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Simple index analytics, which includes the toString() dump of index
 * information.
 * 
 * @author Erich Schubert
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
  public MultiResult processResult(Database<O> db, MultiResult result) {
    Collection<String> header = null;
    if(IndexDatabase.class.isInstance(db)) {
      header = new java.util.Vector<String>();
      header.add(((IndexDatabase<?>) db).toString());
    }
    Collection<Pair<String, String>> col = new java.util.Vector<Pair<String,String>>();
    IndexMetaResult analysis = new IndexMetaResult(col, header);
    result.addResult(analysis);
    return result;
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
      super(col, header);
    }

    @Override
    public String getName() {
      return "index-meta";
    }
  }
}
