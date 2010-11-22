package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Factory for a MkApp-Tree
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkAppTreeFactory<O extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number> extends AbstractMTreeFactory<O, D, MkAppTree<O, D, N>> {
  /**
   * OptionID for {@link #NOLOG_FLAG}
   */
  public static final OptionID NOLOG_ID = OptionID.getOrCreateOptionID("mkapp.nolog", "Flag to indicate that the approximation is done in the ''normal'' space instead of the log-log space (which is default).");

  /**
   * Parameter for nolog
   */
  private final Flag NOLOG_FLAG = new Flag(NOLOG_ID);

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("mkapp.k", "positive integer specifying the maximum number k of reverse k nearest neighbors to be supported.");

  /**
   * Parameter for k
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * OptionID for {@link #P_PARAM}
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("mkapp.p", "positive integer specifying the order of the polynomial approximation.");

  /**
   * Parameter for p
   */
  private final IntParameter P_PARAM = new IntParameter(P_ID, new GreaterConstraint(0));

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * Parameter p.
   */
  private int p;

  /**
   * Flag log.
   */
  private boolean log;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MkAppTreeFactory(Parameterization config) {
    super(config);
    config = config.descend(this);

    if(config.grab(K_PARAM)) {
      k_max = K_PARAM.getValue();
    }
    if(config.grab(P_PARAM)) {
      p = P_PARAM.getValue();
    }
    if(config.grab(NOLOG_FLAG)) {
      log = !NOLOG_FLAG.getValue();
    }
  }

  @Override
  public MkAppTree<O, D, N> instantiate(Database<O> database) {
    return new MkAppTree<O, D, N>(fileName, pageSize, cacheSize, database.getDistanceQuery(distanceFunction), distanceFunction, k_max, p, log);
  }
}
