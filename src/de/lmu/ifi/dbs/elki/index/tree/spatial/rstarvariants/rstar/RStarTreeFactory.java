package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for regular R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has RStarTree oneway - - produces
 *
 * @param <O> Object type
 */
public class RStarTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, RStarTree<O>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RStarTreeFactory(Parameterization config) {
    super(config);
  }

  @Override
  public RStarTree<O> instantiate(Database<O> database) {
    return new RStarTree<O>(database, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }
}
