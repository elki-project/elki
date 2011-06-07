package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Factory for MkMaxTrees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkMaxTreeIndex oneway - - «create»
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkMaxTreeFactory<O, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory<O, D, MkMaxTreeIndex<O, D>> {
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
  public MkMaxTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkMaxTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkMaxTreeIndex<O, D>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction, k_max);
  }

  protected Class<MkMaxTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkMaxTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory.Parameterizer<O, D> {
    @Override
    protected MkMaxTreeFactory<O, D> makeInstance() {
      return new MkMaxTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction, k_max);
    }
  }
}