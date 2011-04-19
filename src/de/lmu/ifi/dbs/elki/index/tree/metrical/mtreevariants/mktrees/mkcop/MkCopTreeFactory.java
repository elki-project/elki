package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
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
 * @apiviz.uses MkCoPTree oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkCopTreeFactory<O, D extends NumberDistance<D, N>, N extends Number> extends AbstractMTreeFactory<O, D, MkCoPTree<O, D, N>> {
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
  public MkCoPTree<O, D, N> instantiate(Relation<O> representation) {
    return new MkCoPTree<O, D, N>(representation, fileName, pageSize, cacheSize, distanceFunction.instantiate(representation), distanceFunction, k_max);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, N>, N extends Number> extends AbstractMTreeFactory.Parameterizer<O, D> {
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
    protected MkCopTreeFactory<O, D, N> makeInstance() {
      return new MkCopTreeFactory<O, D, N>(fileName, pageSize, cacheSize, distanceFunction, k_max);
    }
  }
}