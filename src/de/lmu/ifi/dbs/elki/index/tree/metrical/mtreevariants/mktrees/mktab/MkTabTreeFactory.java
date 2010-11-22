package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for MkTabTrees
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkTabTreeFactory<O extends DatabaseObject, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory<O, D, MkTabTree<O, D>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MkTabTreeFactory(Parameterization config) {
    super(config);
  }

  @Override
  public MkTabTree<O, D> instantiate(Database<O> database) {
    return new MkTabTree<O, D>(fileName, pageSize, cacheSize, database.getDistanceQuery(distanceFunction), distanceFunction, k_max);
  }
}
