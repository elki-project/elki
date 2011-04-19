package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
 * @param <V> Object type
 */
// TODO: Erich: use reproducible random
public class RandomGeneratedReferencePoints<V extends NumberVector<V, ?>> implements ReferencePointsHeuristic<V> {
  /**
   * Parameter to specify the number of requested reference points.
   * <p>
   * Key: {@code -generate.n}
   * </p>
   */
  public static final OptionID N_ID = OptionID.getOrCreateOptionID("generate.n", "The number of reference points to be generated.");

  /**
   * Parameter for additional scaling of the space, to allow out-of-space
   * reference points.
   * <p>
   * Key: {@code -generate.scale}
   * </p>
   */
  public static final OptionID SCALE_ID = OptionID.getOrCreateOptionID("generate.scale", "Scale the grid by the given factor. This can be used to obtain reference points outside the used data space.");

  /**
   * Holds the value of {@link #N_ID}.
   */
  protected int samplesize;

  /**
   * Holds the value of {@link #SCALE_ID}.
   */
  protected double scale = 1.0;

  /**
   * Constructor.
   * 
   * @param samplesize
   * @param scale
   */
  public RandomGeneratedReferencePoints(int samplesize, double scale) {
    super();
    this.samplesize = samplesize;
    this.scale = scale;
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Relation<T> db) {
    Relation<V> database = DatabaseUtil.databaseUglyVectorCast(db);
    Pair<V, V> minmax = DatabaseUtil.computeMinMax(database);
    V factory = DatabaseUtil.assumeVectorField(database).getFactory();

    int dim = DatabaseUtil.dimensionality(db);

    // Compute mean from minmax.
    double[] mean = new double[dim];
    double[] delta = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.doubleValue(d + 1) + minmax.second.doubleValue(d + 1)) / 2;
      delta[d] = (minmax.second.doubleValue(d + 1) - minmax.first.doubleValue(d + 1));
    }

    ArrayList<V> result = new ArrayList<V>(samplesize);
    double[] vec = new double[dim];
    for(int i = 0; i < samplesize; i++) {
      for(int d = 0; d < dim; d++) {
        vec[d] = mean[d] + (Math.random() - 0.5) * scale * delta[d];
      }
      V newp = factory.newInstance(vec);
      // logger.debug("New reference point: " + FormatUtil.format(vec));
      result.add(newp);
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
     * Holds the value of {@link #N_ID}.
     */
    protected int samplesize;

    /**
     * Holds the value of {@link #SCALE_ID}.
     */
    protected double scale = 1.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter samplesizeP = new IntParameter(N_ID, new GreaterConstraint(0));
      if(config.grab(samplesizeP)) {
        samplesize = samplesizeP.getValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID, new GreaterConstraint(0.0), 1.0);
      if(config.grab(scaleP)) {
        scale = scaleP.getValue();
      }
    }

    @Override
    protected RandomGeneratedReferencePoints<V> makeInstance() {
      return new RandomGeneratedReferencePoints<V>(samplesize, scale);
    }
  }
}