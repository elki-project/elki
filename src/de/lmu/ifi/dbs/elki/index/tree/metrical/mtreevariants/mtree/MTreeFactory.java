package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for a M-Tree
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MTree oneway - - «create»
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MTreeFactory<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeFactory<O, D, MTree<O, D>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MTreeFactory(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public MTree<O, D> instantiate(Database<O> database) {
    return new MTree<O, D>(fileName, pageSize, cacheSize, database.getDistanceQuery(distanceFunction), distanceFunction);
  }
}