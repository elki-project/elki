/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * A filter to perturb the values by adding micro-noise.
 * <p>
 * The added noise is generated, attribute-wise, by a Gaussian with mean=0 and a
 * specified standard deviation or by a uniform distribution with a specified
 * range. The standard deviation or the range can be scaled, attribute-wise, to
 * a given percentage of the original standard deviation in the data
 * distribution (assuming a Gaussian distribution there), or to a percentage of
 * the extension in each attribute ({@code maximumValue - minimumValue}).
 * <p>
 * This filter has a potentially wide use but has been implemented for the
 * following publication:
 * <p>
 * Reference:
 * <p>
 * A. Zimek, R. J. G. B. Campello, J. Sander<br>
 * Data Perturbation for Outlier Detection Ensemble<br>
 * Proc. 26th Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM 2014)
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 */
@Title("Data Perturbation for Outlier Detection Ensembles")
@Description("A filter to perturb a datasset on read by an additive noise component, implemented for use in an outlier ensemble (this reference).")
@Reference(authors = "A. Zimek, R. J. G. B. Campello, J. Sander", //
    title = "Data Perturbation for Outlier Detection Ensembles", //
    booktitle = "Proc. 26th International Conference on Scientific and Statistical Database Management (SSDBM), Aalborg, Denmark, 2014", //
    url = "https://doi.org/10.1145/2618243.2618257", //
    bibkey = "DBLP:conf/ssdbm/ZimekCS14")
public class PerturbationFilter<V extends NumberVector> extends AbstractVectorConversionFilter<V, V> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(PerturbationFilter.class);

  /**
   * Scaling reference options.
   * 
   * @author Arthur Zimek
   */
  public enum ScalingReference {
    UNITCUBE, STDDEV, MINMAX
  }

  /**
   * Nature of the noise distribution.
   * 
   * @author Arthur Zimek
   */
  public enum NoiseDistribution {
    GAUSSIAN, UNIFORM
  }

  /**
   * Which reference to use for scaling the noise.
   */
  private ScalingReference scalingreference;

  /**
   * Nature of the noise distribution.
   */
  private NoiseDistribution noisedistribution;

  /**
   * Random object to generate the attribute-wise seeds for the noise.
   */
  private final Random RANDOM;

  /**
   * Percentage of the variance of the random noise generation, given the
   * variance of the corresponding attribute in the data.
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
   * The random objects to generate noise distributions independently for each
   * attribute.
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
   * @param noisedistribution Nature of the noise distribution.
   */
  public PerturbationFilter(Long seed, double percentage, ScalingReference scalingreference, double[] minima, double[] maxima, NoiseDistribution noisedistribution) {
    super();
    this.percentage = percentage;
    this.scalingreference = scalingreference;
    this.minima = minima;
    this.maxima = maxima;
    this.noisedistribution = noisedistribution;
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
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      mvs[d].put(featureVector.doubleValue(d));
    }
  }

  @Override
  protected void prepareComplete() {
    StringBuilder buf = LOG.isDebuggingFine() ? new StringBuilder(1000) : null;
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
          buf.append(' ').append(d).append(": ").append(scalingreferencevalues[d] / percentage);
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
          buf.append(' ').append(d).append(": ").append(scalingreferencevalues[d] / percentage);
        }
      }
    }
    mvs = null;
    if(buf != null) {
      LOG.debugFine(buf.toString());
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
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      if(this.noisedistribution.equals(NoiseDistribution.GAUSSIAN)) {
        values[d] = featureVector.doubleValue(d) + randomPerAttribute[d].nextGaussian() * scalingreferencevalues[d];
      }
      else if(this.noisedistribution.equals(NoiseDistribution.UNIFORM)) {
        values[d] = featureVector.doubleValue(d) + randomPerAttribute[d].nextDouble() * scalingreferencevalues[d];
      }
    }
    return factory.newNumberVector(values);
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Arthur Zimek
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for minimum.
     */
    public static final OptionID MINIMA_ID = new OptionID("perturbationfilter.min", "Only used, if " + ScalingReference.MINMAX + " is set as scaling reference: a comma separated concatenation of the minimum values in each dimension assumed as a reference. If no value is specified, the minimum value of the attribute range in this dimension will be taken.");

    /**
     * Parameter for maximum.
     */
    public static final OptionID MAXIMA_ID = new OptionID("perturbationfilter.max", "Only used, if " + ScalingReference.MINMAX + " is set as scaling reference: a comma separated concatenation of the maximum values in each dimension assumed as a reference. If no value is specified, the maximum value of the attribute range in this dimension will be taken.");

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
     */
    public static final OptionID SEED_ID = new OptionID("perturbationfilter.seed", "Seed for random noise generation.");

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
     */
    public static final OptionID PERCENTAGE_ID = new OptionID("perturbationfilter.percentage", "Percentage of the standard deviation of the random Gaussian noise generation per attribute, given the standard deviation of the corresponding attribute in the original data distribution (assuming a Gaussian distribution there).");

    /**
     * Parameter for selecting scaling reference.
     */
    public static final OptionID SCALINGREFERENCE_ID = new OptionID("perturbationfilter.scalingreference", "The reference for scaling the Gaussian noise. Default is " + ScalingReference.UNITCUBE + ", parameter " + PERCENTAGE_ID.getName() + " will then directly define the standard deviation of all noise Gaussians. For options " + ScalingReference.STDDEV + " and  " + ScalingReference.MINMAX + ", the percentage of the attributewise standard deviation or extension, repectively, will define the attributewise standard deviation of the noise Gaussians.");

    /**
     * Parameter for selecting the noise distribution.
     */
    public static final OptionID NOISEDISTRIBUTION_ID = new OptionID("perturbationfilter.noisedistribution", "The nature of the noise distribution, default is " + NoiseDistribution.UNIFORM);

    /**
     * Percentage of the variance of the random Gaussian noise generation or of
     * the range of the uniform distribution, given the variance of the
     * corresponding attribute in the data.
     */
    protected double percentage;

    /**
     * The option which reference to use for scaling the noise.
     */
    protected ScalingReference scalingreference;

    /**
     * The option which nature of noise distribution to choose.
     */
    protected NoiseDistribution noisedistribution;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<ScalingReference> scalingReferenceP = new EnumParameter<>(SCALINGREFERENCE_ID, ScalingReference.class, ScalingReference.UNITCUBE);
      if(config.grab(scalingReferenceP)) {
        scalingreference = scalingReferenceP.getValue();
      }
      EnumParameter<NoiseDistribution> noisedistributionP = new EnumParameter<>(NOISEDISTRIBUTION_ID, NoiseDistribution.class, NoiseDistribution.UNIFORM);
      if(config.grab(noisedistributionP)) {
        noisedistribution = noisedistributionP.getValue();
      }
      DoubleParameter percentageP = new DoubleParameter(PERCENTAGE_ID, .01) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(percentageP)) {
        percentage = percentageP.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID) //
          .setOptional(true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
      DoubleListParameter minimaP = new DoubleListParameter(MINIMA_ID) //
          .setOptional(true);
      if(config.grab(minimaP)) {
        minima = minimaP.getValue().clone();
      }
      DoubleListParameter maximaP = new DoubleListParameter(MAXIMA_ID) //
          .setOptional(!minimaP.isDefined());
      if(config.grab(maximaP)) {
        maxima = maximaP.getValue().clone();
      }
      // Non-formalized parameter constraint:
      if(minima != null && maxima != null && minima.length != maxima.length) {
        config.reportError(new WrongParameterValueException(minimaP, "and", maximaP, "must have the same number of values."));
      }
    }

    @Override
    protected PerturbationFilter<V> makeInstance() {
      return new PerturbationFilter<>(seed, percentage, scalingreference, minima, maxima, noisedistribution);
    }
  }
}
