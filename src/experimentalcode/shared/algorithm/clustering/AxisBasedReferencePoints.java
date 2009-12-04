package experimentalcode.shared.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Strategy to pick reference points by placing them on the axis ends.
 * 
 * This strategy produces n+2 reference points that lie on the edges of the
 * surrounding cube.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class AxisBasedReferencePoints<O extends NumberVector<O, ?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {
  /**
   * OptionID for {@link #SPACE_SCALE_PARAM}
   */
  public static final OptionID SPACE_SCALE_ID = OptionID.getOrCreateOptionID("axisref.scale", "Scale the data space extension by the given factor.");

  /**
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -axisref.scale}
   * </p>
   */
  private final DoubleParameter SPACE_SCALE_PARAM = new DoubleParameter(SPACE_SCALE_ID, new GreaterEqualConstraint(0.0), 1.0);

  /**
   * Holds the value of {@link #SPACE_SCALE_PARAM}.
   */
  protected double spacescale;

  /**
   * Constructor, Parameterizable style.
   */
  public AxisBasedReferencePoints() {
    super();
    addOption(SPACE_SCALE_PARAM);
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    Pair<O, O> minmax = DatabaseUtil.computeMinMax(db);
    O prototype = minmax.first;

    int dim = db.dimensionality();

    // Compute mean and extend from minmax.
    double[] mean = new double[dim];
    double[] delta = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.getValue(d + 1).doubleValue() + minmax.second.getValue(d + 1).doubleValue()) / 2;
      delta[d] = spacescale * (minmax.second.getValue(d + 1).doubleValue() - mean[d]);
    }

    ArrayList<O> result = new ArrayList<O>(2 + dim);

    double[] vec = new double[dim];
    // Use min and max
    for(int d = 0; d < dim; d++) {
      vec[d] = mean[d] - delta[d];
    }
    result.add(prototype.newInstance(vec));
    for(int d = 0; d < dim; d++) {
      vec[d] = mean[d] + delta[d];
    }
    result.add(prototype.newInstance(vec));

    // Plus axis end points:
    for(int i = 0; i < dim; i++) {
      for(int d = 0; d < dim; d++) {
        if(d != i) {
          vec[d] = mean[d] - delta[d];
        }
        else {
          vec[d] = mean[d] + delta[d];
        }
      }
      result.add(prototype.newInstance(vec));
    }

    return result;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    spacescale = SPACE_SCALE_PARAM.getValue();

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
