package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Factory for a MkCoPTree-Tree
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkCoPTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkCopTreeFactory<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory<O, D, MkCoPTreeIndex<O, D>> {
  /**
   * Parameter for k
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("mkcop.k", "positive integer specifying the maximum number k of reverse k nearest neighbors to be supported.");

  /**
   * Parameter k.
   */
  int k_max;

  /**
   * Constructor.
   *
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   * @param k_max
   */
  public MkCopTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(fileName, pageSize, cacheSize, distanceFunction);
    this.k_max = k_max;
  }

  @Override
  public MkCoPTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkCoPTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkCoPTreeIndex<O, D>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction, k_max);
  }

  protected Class<MkCoPTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkCoPTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory.Parameterizer<O, D> {
    protected int k_max = 0;
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_ID, new GreaterConstraint(0));
      if (config.grab(k_maxP)) {
        k_max = k_maxP.getValue();
      }
    }

    @Override
    protected MkCopTreeFactory<O, D> makeInstance() {
      return new MkCopTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction, k_max);
    }
  }
}