package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract factory for various Mk-Trees
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses AbstractMkTree oneway - - «create»
 *
 * @param <O> Object type
 * @param <D> Distance type
 * @param <I> Index type
 */
public abstract class AbstractMkTreeUnifiedFactory<O extends DatabaseObject, D extends Distance<D>, I extends AbstractMkTreeUnified<O, D, ?, ?>> extends AbstractMTreeFactory<O, D, I> {
  /**
   * Parameter specifying the maximal number k of reverse k nearest neighbors to
   * be supported, must be an integer greater than 0.
   * <p>
   * Key: {@code -mktree.kmax}
   * </p>
   */
  public static final OptionID K_MAX_ID = OptionID.getOrCreateOptionID("mktree.kmax", "Specifies the maximal number k of reverse k nearest neighbors to be supported.");

  /**
   * Holds the value of parameter {@link #K_MAX_ID}.
   */
  protected int k_max;

  /**
   * Constructor.
   *
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   * @param k_max
   */
  public AbstractMkTreeUnifiedFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(fileName, pageSize, cacheSize, distanceFunction);
    this.k_max = k_max;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeFactory.Parameterizer<O, D> {
    protected int k_max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_MAX_ID, new GreaterConstraint(0));

      if (config.grab(k_maxP)) {
        k_max = k_maxP.getValue();
      }
    }

    @Override
    protected abstract AbstractMkTreeUnifiedFactory<O, D, ?> makeInstance();
  }
}