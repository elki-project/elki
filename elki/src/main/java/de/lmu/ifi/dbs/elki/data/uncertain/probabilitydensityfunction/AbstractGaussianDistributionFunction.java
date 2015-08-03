package de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction;

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Abstract class for gaussian models for use in uncertain objects.
 *
 * Primary target was to reduce code duplicity regarding multivariate gaussian
 * distributions and dimensional independent gaussian distributions.
 *
 * Those two cases are separated to not use to fat (containing {@link List} of
 * {@link Matrix} for simple univariate gaussian distributions per default.
 *
 * @author Alexander Koos
 */
public abstract class AbstractGaussianDistributionFunction<V, F extends AbstractGaussianDistributionFunction<V, F>> extends ProbabilityDensityFunction<F> {

  /**
   * Field to hold the value the randomly created variance shall have in
   * minimum.
   */
  protected double minDev;

  /**
   * Field to hold the value the randomly created variance shall have in
   * maximum.
   */
  protected double maxDev;

  /**
   * Field to hold the value the randomly created maximum negative deviation
   * from the groundtruth shall have in minimum.
   */
  protected double minMin;

  /**
   * Field to hold the value the randomly created maximum negative deviation
   * from the groundtruth shall have in maximum.
   */
  protected double maxMin;

  /**
   * Field to hold the value the randomly created maximum positive deviation
   * from the groundtruth shall have in minimum.
   */
  protected double minMax;

  /**
   * Field to hold the value the randomly created maximum positive deviation
   * from the groundtruth shall have in maximum.
   */
  protected double maxMax;

  /**
   * Field to hold the value the randomly created multiplicity shall have in
   * minimum.
   */
  protected int multMin;

  /**
   * Field to hold the value the randomly created multiplicity shall have in
   * maximum.
   */
  protected int multMax;

  /**
   * Field to hold a {@link Random} used for uncertainification.
   */
  protected Random urand;

  protected List<DoubleVector> means;

  protected List<V> variances;

  protected int[] weights;

  protected int weightMax = UncertainObject.PROBABILITY_SCALE; // reset by constructor
  // in case no bounded sample has been found

  protected static DoubleVector noSample = new DoubleVector(new double[] { Double.NEGATIVE_INFINITY });

  public void setMeans(List<DoubleVector> means) {
    this.means = means;
  }

  public List<DoubleVector> getMeans() {
    return this.means;
  }

  public DoubleVector getMean(int position) {
    return this.means.get(position);
  }

  public void setMean(int position, DoubleVector mean) {
    this.means.set(position, mean);
  }

  public void setWeights(int[] weights) {
    this.weights = weights;
  }

  public int[] getWeights() {
    return this.weights;
  }

  public int getWeight(int position) {
    return this.weights[position];
  }

  public void setVariances(List<V> variances) {
    this.variances = variances;
  }

  public List<V> getVariances() {
    return this.variances;
  }

  public V getVariances(int position) {
    return this.variances.get(position);
  }

  protected abstract List<DoubleVector> getDeviationVector();

  protected SpatialComparable getDefaultBounds(int dimensions, List<DoubleVector> means, List<DoubleVector> deviationVector) {
    double[] min = new double[dimensions];
    double[] max = new double[dimensions];
    double localExtreme;
    for(int i = 0; i < dimensions; i++) {
      min[i] = Double.MAX_VALUE;
      max[i] = Double.MIN_VALUE;
    }
    for(int i = 0; i < means.size(); i++) {
      for(int j = 0; j < dimensions; j++) {
        localExtreme = means.get(i).doubleValue(j) - deviationVector.get(i).doubleValue((deviationVector.get(i).getDimensionality() > 1 ? j : 0)) * 3;
        min[j] = localExtreme < min[j] ? localExtreme : min[j];
        localExtreme = means.get(i).doubleValue(j) + deviationVector.get(i).doubleValue((deviationVector.get(i).getDimensionality() > 1 ? j : 0)) * 3;
        max[j] = localExtreme > max[j] ? localExtreme : max[j];
      }
    }
    return new HyperBoundingBox(min, max);
  }

  @Override
  public SpatialComparable getDefaultBounds(int dimensions) {
    return getDefaultBounds(dimensions, getMeans(), getDeviationVector());
  }
}
