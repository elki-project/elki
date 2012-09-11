package experimentalcode.arthur;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.AllOrNoneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualSizeGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * A filter to perturb the values by adding Gaussian micro-noise.
 * 
 * The added noise is generated, attribute-wise, by a Gaussian with mean=0 and a
 * standard deviation. The standard deviation can be scaled, attribute-wise, to
 * a given percentage of the original standard deviation in the data
 * distribution (assuming a Gaussian distribution there), or to a percentage of
 * the extension in each attribute ({@code maximumValue - minimumValue}).
 * 
 * @author Arthur Zimek
 */
public class PerturbationFilter<V extends NumberVector<?>> extends AbstractVectorConversionFilter<V, V> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(PerturbationFilter.class);

  /**
   * Scaling reference options.
   * 
   * @author Arthur Zimek
   * 
   * @apiviz.exclude
   */
  public static enum ScalingReference {
    UNITCUBE, STDDEV, MINMAX
  }

  /**
   * Which reference to use for scaling the noise.
   */
  private ScalingReference scalingreference;

  /**
   * Random object to generate the attribute-wise seeds for the Gaussian noise.
   */
  private final Random RANDOM;

  /**
   * Percentage of the variance of the random Gaussian noise generation, given
   * the variance of the corresponding attribute in the data.
   */
  private double percentage;

  /**
   * Temporary storage used during initialization.
   */
  private MeanVarianceMinMax[] mvs = null;

  /**
   * Stores the scaling reference in each dimension.
   */
  private double[] scalingreferencevalues = new double[0];

  /**
   * The random objects to generate Gaussians independently for each attribute.
   */
  private Random[] randomPerAttribute = null;

  /**
   * Stores the maximum in each dimension.
   */
  private double[] maxima;

  /**
   * Stores the minimum in each dimension.
   */
  private double[] minima;

  /**
   * Stores the dimensionality from the preprocessing.
   */
  private int dimensionality = 0;

  /**
   * Constructor.
   * 
   * @param seed Seed value, may be {@code null} for a random seed.
   * @param percentage Relative amount of jitter to add
   * @param scalingreference Scaling reference
   * @param minima Preset minimum values. May be {@code null}.
   * @param maxima Preset maximum values. May be {@code null}.
   */
  public PerturbationFilter(Long seed, double percentage, ScalingReference scalingreference, double[] minima, double[] maxima) {
    super();
    this.percentage = percentage;
    this.scalingreference = scalingreference;
    this.minima = minima;
    this.maxima = maxima;
    this.RANDOM = (seed == null) ? new Random() : new Random(seed);
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    if(scalingreference == ScalingReference.MINMAX && minima.length != 0 && maxima.length != 0) {
      dimensionality = minima.length;
      scalingreferencevalues = new double[dimensionality];
      randomPerAttribute = new Random[dimensionality];
      for(int d = 0; d < dimensionality; d++) {
        scalingreferencevalues[d] = (maxima[d] - minima[d]) * percentage;
        if(scalingreferencevalues[d] == 0 || Double.isNaN(scalingreferencevalues[d])) {
          scalingreferencevalues[d] = percentage;
        }
        randomPerAttribute[d] = new Random(RANDOM.nextLong());
      }
      return false;
    }
    if(scalingreference == ScalingReference.UNITCUBE) {
      return false;
    }
    return (scalingreferencevalues.length == 0);
  }

  @Override
  protected void prepareProcessInstance(V featureVector) {
    // First object? Then init. (We didn't have a dimensionality before!)
    if(mvs == null) {
      dimensionality = featureVector.getDimensionality();
      mvs = MeanVarianceMinMax.newArray(dimensionality);
    }
    for(int d = 1; d <= featureVector.getDimensionality(); d++) {
      mvs[d - 1].put(featureVector.doubleValue(d));
    }
  }

  @Override
  protected void prepareComplete() {
    StringBuffer buf = logger.isDebuggingFine() ? new StringBuffer() : null;
    scalingreferencevalues = new double[dimensionality];
    randomPerAttribute = new Random[dimensionality];
    if(scalingreference == ScalingReference.STDDEV) {
      if(buf != null) {
        buf.append("Standard deviation per attribute: ");
      }
      for(int d = 0; d < dimensionality; d++) {
        scalingreferencevalues[d] = mvs[d].getSampleStddev() * percentage;
        if(scalingreferencevalues[d] == 0 || Double.isNaN(scalingreferencevalues[d])) {
          scalingreferencevalues[d] = percentage;
        }
        randomPerAttribute[d] = new Random(RANDOM.nextLong());
        if(buf != null) {
          buf.append(" ").append(d).append(": ").append(scalingreferencevalues[d] / percentage);
        }
      }
    }
    else if(scalingreference == ScalingReference.MINMAX && minima.length == 0 && maxima.length == 0) {
      if(buf != null) {
        buf.append("extension per attribute: ");
      }
      for(int d = 0; d < dimensionality; d++) {
        scalingreferencevalues[d] = (mvs[d].getMax() - mvs[d].getMin()) * percentage;
        if(scalingreferencevalues[d] == 0 || Double.isNaN(scalingreferencevalues[d])) {
          scalingreferencevalues[d] = percentage;
        }
        randomPerAttribute[d] = new Random(RANDOM.nextLong());
        if(buf != null) {
          buf.append(" ").append(d).append(": ").append(scalingreferencevalues[d] / percentage);
        }
      }
    }
    mvs = null;
    if(buf != null) {
      logger.debugFine(buf.toString());
    }
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    if(scalingreference == ScalingReference.UNITCUBE && dimensionality == 0) {
      dimensionality = featureVector.getDimensionality();
      scalingreferencevalues = new double[dimensionality];
      randomPerAttribute = new Random[dimensionality];
      for(int d = 0; d < dimensionality; d++) {
        scalingreferencevalues[d] = percentage;
        randomPerAttribute[d] = new Random(RANDOM.nextLong());
      }
    }
    if(scalingreferencevalues.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors and given Minima/Maxima differ in length.");
    }
    double[] values = new double[featureVector.getDimensionality()];
    for(int d = 1; d <= featureVector.getDimensionality(); d++) {
      values[d - 1] = featureVector.doubleValue(d) + randomPerAttribute[d - 1].nextGaussian() * scalingreferencevalues[d - 1];
    }
    return factory.newNumberVector(values);
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  /**
   * Parameterization class.
   * 
   * @author Arthur Zimek
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter for minimum.
     */
    public static final OptionID MINIMA_ID = OptionID.getOrCreateOptionID("perturbationfilter.min", "Only used, if " + ScalingReference.MINMAX + " is set as scaling reference: a comma separated concatenation of the minimum values in each dimension assumed as a reference. If no value is specified, the minimum value of the attribute range in this dimension will be taken.");

    /**
     * Parameter for maximum.
     */
    public static final OptionID MAXIMA_ID = OptionID.getOrCreateOptionID("perturbationfilter.max", "Only used, if " + ScalingReference.MINMAX + " is set as scaling reference: a comma separated concatenation of the maximum values in each dimension assumed as a reference. If no value is specified, the maximum value of the attribute range in this dimension will be taken.");

    /**
     * Stores the maximum in each dimension.
     */
    private double[] maxima = new double[0];

    /**
     * Stores the minimum in each dimension.
     */
    private double[] minima = new double[0];

    /**
     * Optional parameter to specify a seed for random Gaussian noise
     * generation. If unused, system time is used as seed.
     * <p>
     * Key: {@code -perturbationfilter.seed}
     * </p>
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("perturbationfilter.seed", "Seed for random noise generation.");

    /**
     * Seed for randomly shuffling the rows of the database. If null, system
     * time is used as seed.
     */
    protected Long seed = null;

    /**
     * Optional parameter to specify a percentage of the standard deviation of
     * the random Gaussian noise generation, given the standard deviation of the
     * corresponding attribute in the original data distribution (assuming a
     * Gaussian there).
     * 
     * <p>
     * Key: {@code -perturbationfilter.percentage}
     * </p>
     * <p>
     * Default: <code>0.01</code>
     * </p>
     * <p>
     * Constraint: 0 &lt; percentage &leq;1
     * </p>
     */
    public static final OptionID PERCENTAGE_ID = OptionID.getOrCreateOptionID("perturbationfilter.percentage", "Percentage of the standard deviation of the random Gaussian noise generation per attribute, given the standard deviation of the corresponding attribute in the original data distribution (assuming a Gaussian distribution there).");

    /**
     * Parameter for selecting scaling reference.
     * <p>
     * Key: {@code -perturbationfilter.scalingreference}
     * </p>
     * <p>
     * Default: <code>ScalingReference.UNITCUBE</code>
     * </p>
     */
    public static final OptionID SCALINGREFERENCE_ID = OptionID.getOrCreateOptionID("perturbationfilter.scalingreference", "The reference for scaling the Gaussian noise. Default is " + ScalingReference.UNITCUBE + ", parameter " + PERCENTAGE_ID.getName() + " will then directly define the standard deviation of all noise Gaussians. For options " + ScalingReference.STDDEV + " and  " + ScalingReference.MINMAX + ", the percentage of the attributewise standard deviation or extension, repectively, will define the attributewise standard deviation of the noise Gaussians.");

    /**
     * Percentage of the variance of the random Gaussian noise generation, given
     * the variance of the corresponding attribute in the data.
     */
    protected double percentage;

    /**
     * The option which reference to use for scaling the noise.
     */
    protected ScalingReference scalingreference;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<ScalingReference> scalingReferenceP = new EnumParameter<ScalingReference>(SCALINGREFERENCE_ID, ScalingReference.class, ScalingReference.UNITCUBE);
      if(config.grab(scalingReferenceP)) {
        scalingreference = scalingReferenceP.getValue();
      }
      DoubleParameter percentageP = new DoubleParameter(PERCENTAGE_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE), .01);
      if(config.grab(percentageP)) {
        percentage = percentageP.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
      DoubleListParameter minimaP = new DoubleListParameter(MINIMA_ID, true);
      if(config.grab(minimaP)) {
        minima = ArrayLikeUtil.toPrimitiveDoubleArray(minimaP.getValue());
      }
      DoubleListParameter maximaP = new DoubleListParameter(MAXIMA_ID, true);
      if(config.grab(maximaP)) {
        maxima = ArrayLikeUtil.toPrimitiveDoubleArray(maximaP.getValue());
      }

      ArrayList<Parameter<?, ?>> globalSetMinAndMax = new ArrayList<Parameter<?, ?>>();
      globalSetMinAndMax.add(minimaP);
      globalSetMinAndMax.add(maximaP);
      config.checkConstraint(new AllOrNoneMustBeSetGlobalConstraint(globalSetMinAndMax));

      ArrayList<ListParameter<?>> globalMinMaxEqualsize = new ArrayList<ListParameter<?>>();
      globalMinMaxEqualsize.add(minimaP);
      globalMinMaxEqualsize.add(maximaP);
      config.checkConstraint(new EqualSizeGlobalConstraint(globalMinMaxEqualsize));
    }

    @Override
    protected PerturbationFilter<V> makeInstance() {
      return new PerturbationFilter<V>(seed, percentage, scalingreference, minima, maxima);
    }
  }
}