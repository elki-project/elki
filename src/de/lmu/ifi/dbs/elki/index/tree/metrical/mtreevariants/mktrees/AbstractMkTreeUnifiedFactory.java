package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
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
   * OptionID for {@link #K_MAX_PARAM}.
   */
  public static final OptionID K_MAX_ID = OptionID.getOrCreateOptionID("mktree.kmax", "Specifies the maximal number k of reverse k nearest neighbors to be supported.");

  /**
   * Parameter specifying the maximal number k of reverse k nearest neighbors to
   * be supported, must be an integer greater than 0.
   * <p>
   * Key: {@code -mktree.kmax}
   * </p>
   */
  public final IntParameter K_MAX_PARAM = new IntParameter(K_MAX_ID, new GreaterConstraint(0));

  /**
   * Holds the value of parameter {@link #K_MAX_PARAM}.
   */
  protected int k_max;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractMkTreeUnifiedFactory(Parameterization config) {
    super(config);
    config = config.descend(this);
    if (config.grab(K_MAX_PARAM)) {
      k_max = K_MAX_PARAM.getValue();
    }
  }
}
