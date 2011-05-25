package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for a M-Tree
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MTreeFactory<O, D extends Distance<D>> extends AbstractMTreeFactory<O, D, MTreeIndex<O, D>> {
  /**
   * Constructor.
   * 
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param distanceFunction Distance function
   */
  public MTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction) {
    super(fileName, pageSize, cacheSize, distanceFunction);
  }

  @Override
  public MTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MTreeIndex<O, D>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction);
  }

  protected Class<MTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractMTreeFactory.Parameterizer<O, D> {
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