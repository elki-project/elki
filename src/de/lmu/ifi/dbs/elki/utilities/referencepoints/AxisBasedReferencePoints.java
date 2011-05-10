package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -axisref.scale}
   * </p>
   */
  public static final OptionID SPACE_SCALE_ID = OptionID.getOrCreateOptionID("axisref.scale", "Scale the data space extension by the given factor.");

  /**
   * Holds the value of {@link #SPACE_SCALE_ID}.
   */
  protected double spacescale;

  /**
   * Constructor.
   * 
   * @param spacescale
   */
  public AxisBasedReferencePoints(double spacescale) {
    super();
    this.spacescale = spacescale;
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Relation<T> db) {
    Relation<V> database = DatabaseUtil.relationUglyVectorCast(db);
    Pair<V, V> minmax = DatabaseUtil.computeMinMax(database);
    V factory = DatabaseUtil.assumeVectorField(database).getFactory();

    int dim = DatabaseUtil.dimensionality(db);

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
    result.add(factory.newInstance(vec));
    for(int d = 0; d < dim; d++) {
      vec[d] = mean[d] + delta[d];
    }
    result.add(factory.newInstance(vec));

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
      result.add(factory.newInstance(vec));
    }

    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    /**
     * Holds the value of {@link #SPACE_SCALE_ID}.
     */
    protected double spacescale = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter spacescaleP = new DoubleParameter(SPACE_SCALE_ID, new GreaterEqualConstraint(0.0), 1.0);
      if(config.grab(spacescaleP)) {
        spacescale = spacescaleP.getValue();
      }
    }

    @Override
    protected AxisBasedReferencePoints<V> makeInstance() {
      return new AxisBasedReferencePoints<V>(spacescale);
    }
  }
}