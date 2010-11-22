package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for flat R*-Trees.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public class FlatRStarTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, FlatRStarTree<O>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public FlatRStarTreeFactory(Parameterization config) {
    super(config);
  }

  @Override
  public FlatRStarTree<O> instantiate(Database<O> database) {
    return new FlatRStarTree<O>(database, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }
}
