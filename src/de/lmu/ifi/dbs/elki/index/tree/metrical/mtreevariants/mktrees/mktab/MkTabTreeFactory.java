package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Factory for MkTabTrees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkTabTree oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkTabTreeFactory<O, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory<O, D, MkTabTreeIndex<O, D>> {
  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   * @param k_max
   */
  public MkTabTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(fileName, pageSize, cacheSize, distanceFunction, k_max);
  }

  @Override
  public MkTabTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkTabTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkTabTreeIndex<O, D>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction, k_max);
  }

  protected Class<MkTabTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkTabTreeNode.class);
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
    protected MkTabTreeFactory<O, D> makeInstance() {
      return new MkTabTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction, k_max);
    }
  }
}