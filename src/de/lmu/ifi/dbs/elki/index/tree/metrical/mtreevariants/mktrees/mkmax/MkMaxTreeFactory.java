package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for MkMaxTrees
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkMaxTreeFactory<O extends DatabaseObject, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory<O, D, MkMaxTree<O, D>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MkMaxTreeFactory(Parameterization config) {
    super(config);
  }

  @Override
  public MkMaxTree<O, D> instantiate(Database<O> database) {
    return new MkMaxTree<O, D>(fileName, pageSize, cacheSize, database.getDistanceQuery(distanceFunction), distanceFunction, k_max);
  }
}
