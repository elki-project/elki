package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
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
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   */
  public MTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction) {
    super(fileName, pageSize, cacheSize, distanceFunction);
  }

  @Override
  public MTree<O, D> instantiate(Database<O> database) {
    return new MTree<O, D>(fileName, pageSize, cacheSize, database.getDistanceQuery(distanceFunction), distanceFunction);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeFactory.Parameterizer<O, D> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected MTreeFactory<O, D> makeInstance() {
      return new MTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction);
    }
  }
}