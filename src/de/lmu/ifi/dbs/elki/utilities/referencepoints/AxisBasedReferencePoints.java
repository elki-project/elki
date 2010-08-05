package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Strategy to pick reference points by placing them on the axis ends.
 * 
 * This strategy produces n+2 reference points that lie on the edges of the
 * surrounding cube.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class AxisBasedReferencePoints<V extends NumberVector<V, ?>> implements ReferencePointsHeuristic<V> {
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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AxisBasedReferencePoints(Parameterization config) {
    super();
    config = config.descend(this);
    if (config.grab(SPACE_SCALE_PARAM)) {
      spacescale = SPACE_SCALE_PARAM.getValue();
    }
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Database<T> db) {
    Database<V> database = DatabaseUtil.databaseUglyVectorCast(db);
    Pair<V, V> minmax = DatabaseUtil.computeMinMax(database);
    V prototype = minmax.first;

    int dim = db.dimensionality();

    // Compute mean and extend from minmax.
    double[] mean = new double[dim];
    double[] delta = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.doubleValue(d + 1) + minmax.second.doubleValue(d + 1)) / 2;
      delta[d] = spacescale * (minmax.second.doubleValue(d + 1) - mean[d]);
    }

    ArrayList<V> result = new ArrayList<V>(2 + dim);

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
}