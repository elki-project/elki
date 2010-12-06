package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for DeLiClu R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses DeLiCluTree oneway - - «create»
 *
 * @param <O> Object type
 */
public class DeLiCluTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, DeLiCluTree<O>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DeLiCluTreeFactory(Parameterization config) {
    super(config);
  }

  @Override
  public DeLiCluTree<O> instantiate(Database<O> database) {
    return new DeLiCluTree<O>(database, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }
}
