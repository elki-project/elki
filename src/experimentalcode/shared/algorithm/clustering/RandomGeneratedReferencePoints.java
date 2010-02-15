package experimentalcode.shared.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Reference points generated randomly within the used data space.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class RandomGeneratedReferencePoints<O extends NumberVector<O, ?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {
  /**
   * OptionID for {@link #N_PARAM}
   */
  public static final OptionID N_ID = OptionID.getOrCreateOptionID("generate.n", "The number of reference points to be generated.");

  /**
   * Parameter to specify the number of requested reference points.
   * <p>
   * Key: {@code -generate.n}
   * </p>
   */
  private final IntParameter N_PARAM = new IntParameter(N_ID, new GreaterConstraint(0));

  /**
   * OptionID for {@link #SCALE_PARAM}
   */
  public static final OptionID SCALE_ID = OptionID.getOrCreateOptionID("generate.scale", "Scale the grid by the given factor. This can be used to obtain reference points outside the used data space.");

  /**
   * Parameter for additional scaling of the space, to allow out-of-space
   * reference points.
   * <p>
   * Key: {@code -generate.scale}
   * </p>
   */
  private final DoubleParameter SCALE_PARAM = new DoubleParameter(SCALE_ID, new GreaterConstraint(0.0), 1.0);

  /**
   * Holds the value of {@link #N_PARAM}.
   */
  protected int samplesize;

  /**
   * Holds the value of {@link #SCALE_PARAM}.
   */
  protected double scale;

  /**
   * Constructor, Parameterizable style.
   */
  public RandomGeneratedReferencePoints(Parameterization config) {
    super();
    if(config.grab(this, N_PARAM)) {
      samplesize = N_PARAM.getValue();
    }
    if(config.grab(this, SCALE_PARAM)) {
      scale = SCALE_PARAM.getValue();
    }
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    Pair<O, O> minmax = DatabaseUtil.computeMinMax(db);
    O prototype = minmax.first;

    int dim = db.dimensionality();

    // Compute mean from minmax.
    double[] mean = new double[dim];
    double[] delta = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.doubleValue(d + 1) + minmax.second.doubleValue(d + 1)) / 2;
      delta[d] = (minmax.second.doubleValue(d + 1) - minmax.first.doubleValue(d + 1));
    }

    ArrayList<O> result = new ArrayList<O>(samplesize);
    double[] vec = new double[dim];
    for(int i = 0; i < samplesize; i++) {
      for(int d = 0; d < dim; d++) {
        vec[d] = mean[d] + (Math.random() - 0.5) * scale * delta[d];
      }
      O newp = prototype.newInstance(vec);
      // logger.debug("New reference point: " + FormatUtil.format(vec));
      result.add(newp);
    }

    return result;
  }
}
