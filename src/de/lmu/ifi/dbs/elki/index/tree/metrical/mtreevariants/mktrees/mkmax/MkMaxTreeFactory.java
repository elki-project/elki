package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;

/**
 * Factory for MkMaxTrees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkMaxTree oneway - - «create»
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkMaxTreeFactory<O extends DatabaseObject, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory<O, D, MkMaxTree<O, D>> {
  /**
   * Constructor.
   *
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   * @param k_max
   */
  public MkMaxTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(fileName, pageSize, cacheSize, distanceFunction, k_max);
  }

  @Override
  public MkMaxTree<O, D> instantiate(Database<O> database) {
    return new MkMaxTree<O, D>(fileName, pageSize, cacheSize, database.getDistanceQuery(distanceFunction), distanceFunction, k_max);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory.Parameterizer<O, D> {
    @Override
    protected MkMaxTreeFactory<O, D> makeInstance() {
      return new MkMaxTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction, k_max);
    }
  }
}