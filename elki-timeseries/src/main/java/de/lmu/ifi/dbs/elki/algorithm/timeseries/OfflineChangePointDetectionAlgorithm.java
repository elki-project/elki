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
package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Off-line change point detection algorithm detecting a change in mean, based
 * on the cumulative sum (CUSUM), same-variance assumption, and using bootstrap
 * sampling for significance estimation.
 * <p>
 * References:
 * <p>
 * D. Picard<br>
 * Testing and Estimating Change-Points in Time Series<br>
 * Advances in Applied Probability Vol. 17
 * <p>
 * early results along these lines can be found in:
 * <p>
 * E. S. Page<br>
 * On Problems in which a Change in a Parameter Occurs at an Unknown Point<br>
 * Biometrika Vol. 44
 * <p>
 * also discussed in:
 * <p>
 * M. Basseville and I. V. Nikiforov<br>
 * Section 2.6: Off-line Change Detection<br>
 * Detection of Abrupt Changes - Theory and Application<br>
 *
 * @author Sebastian Rühl
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - ChangePoints
 */
@Title("Off-line Change Point Detection")
@Description("Detects multiple change points in a time series")
@Reference(authors = "D. Picard", //
    title = "Testing and Estimating Change-Points in Time Series ", //
    booktitle = "Advances in Applied Probability Vol. 17", //
    url = "https://doi.org/10.2307/1427090", //
    bibkey = "doi:10.2307/1427090")
@Reference(authors = "E. S. Page", //
    title = "On Problems in which a Change in a Parameter Occurs at an Unknown Point", //
    booktitle = "Biometrika Vol. 44", //
    url = "https://doi.org/10.2307/2333258", //
    bibkey = "doi:10.2307/2333258")
@Reference(authors = "M. Basseville, I. V. Nikiforov", //
    title = "Section 2.6: Off-line Change Detection", //
    booktitle = "Detection of Abrupt Changes - Theory and Application", //
    url = "http://people.irisa.fr/Michele.Basseville/kniga/kniga.pdf", //
    bibkey = "books/prentice/BassevilleN93/C2")
public class OfflineChangePointDetectionAlgorithm extends AbstractAlgorithm<ChangePoints> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(OfflineChangePointDetectionAlgorithm.class);

  /**
   * Number of samples for bootstrap significance.
   */
  int bootstrapSamples;

  /**
   * Mininum confidence.
   */
  double minConfidence;

  /**
   * Random generator
   */
  RandomFactory rnd;

  /**
   * Constructor
   *
   * @param confidence Confidence
   * @param bootstrapSteps Steps for bootstrapping
   */
  public OfflineChangePointDetectionAlgorithm(double confidence, int bootstrapSteps, RandomFactory rnd) {
    this.minConfidence = confidence;
    this.bootstrapSamples = bootstrapSteps;
    this.rnd = rnd;
  }

  /**
   * Executes multiple change point detection for given relation
   *
   * @param relation the relation to process
   * @return list with all the detected change point for every time series
   */
  public ChangePoints run(Relation<DoubleVector> relation) {
    if(!(relation.getDBIDs() instanceof ArrayDBIDs)) {
      throw new AbortException("This implementation may only be used on static databases, with ArrayDBIDs to provide a clear order.");
    }
    return new Instance(rnd.getSingleThreadedRandom()).run(relation);
  }

  /**
   * Instance for a single data set.
   * 
   * @author Erich Schubert
   */
  class Instance {
    /**
     * Raw data column.
     */
    double[] column;

    /**
     * Cumulative sum.
     */
    double[] sums;

    /**
     * Temporary storage for bootstrap testing.
     */
    double[] bstrap;

    /**
     * Iterator to reference data positions.
     */
    DBIDArrayIter iter;

    /**
     * Result to output to.
     */
    ChangePoints result;

    /**
     * Current column number.
     */
    int columnnr;

    /**
     * Random generator.
     */
    Random rnd;

    /**
     * Constructor.
     *
     * @param rnd Random generator
     */
    public Instance(Random rnd) {
      this.rnd = rnd;
    }

    /**
     * Run the change point detection algorithm on a data relation.
     * 
     * @param relation Data relation.
     * @return Change points
     */
    public ChangePoints run(Relation<DoubleVector> relation) {
      final ArrayDBIDs ids = (ArrayDBIDs) relation.getDBIDs();
      final int dim = RelationUtil.dimensionality(relation);
      final int size = ids.size();
      iter = ids.iter();

      column = new double[size];
      sums = new double[size];
      bstrap = new double[size];
      result = new ChangePoints("CUSUM Changepoints", "cusum-changepoints");

      for(columnnr = 0; columnnr < dim; columnnr++) {
        // Materialize one column of the data.
        for(iter.seek(0); iter.valid(); iter.advance()) {
          column[iter.getOffset()] = relation.get(iter).doubleValue(columnnr);
        }
        cusum(column, sums, 0, size);
        multipleChangepointsWithConfidence(0, size);
      }
      return result;
    }

    /**
     * Performs multiple change point detection for a given time series. This
     * method uses a kind of divide and conquer approach
     *
     * @param begin Interval begin
     * @param end Interval end
     * @return Last change point position, or begin
     */
    private int multipleChangepointsWithConfidence(int begin, int end) {
      if(end - begin <= 3) {
        return begin; // Too short.
      }
      DoubleIntPair change = bestChangeInMean(sums, begin, end);
      double confidence = bootstrapConfidence(begin, end, change.first);
      // return the detected changepoint
      if(confidence < minConfidence) {
        return begin; // Stop.
      }
      // Divide and Conquer:
      multipleChangepointsWithConfidence(begin, change.second);
      result.add(iter.seek(change.second), columnnr, confidence);
      return multipleChangepointsWithConfidence(change.second, end);
    }

    /**
     * Calculates the confidence for the most probable change point of the given
     * timer series. Confidence is calculated with the help of bootstrapping.
     *
     * @param begin Subset begin
     * @param end Subset end
     * @param thresh Threshold
     * @return confidence for most probable change point
     */
    private double bootstrapConfidence(int begin, int end, double thresh) {
      final int len = end - begin;
      int pos = 0;
      for(int i = 0; i < bootstrapSamples; i++) {
        System.arraycopy(column, begin, bstrap, 0, len);
        shuffle(bstrap, len, rnd);
        cusum(bstrap, bstrap, 0, len);
        double score = bestChangeInMean(bstrap, 0, len).first;
        if(score < thresh) {
          ++pos;
        }
      }
      return pos / (double) bootstrapSamples;
    }
  }

  /**
   * Compute the incremental sum of an array, i.e. the sum of all points up to
   * the given index.
   *
   * @param data Input data
   * @param out Output array (must be large enough).
   */
  public static void cusum(double[] data, double[] out, int begin, int end) {
    assert (out.length >= data.length);
    // Use Kahan summation for better precision!
    // FIXME: this should be unit tested.
    double m = 0., carry = 0.;
    for(int i = begin; i < end; i++) {
      double v = data[i] - carry; // Compensation
      double n = out[i] = (m + v); // May lose small digits of v.
      carry = (n - m) - v; // Recover lost bits
      m = n;
    }
  }

  /**
   * Find the best position to assume a change in mean.
   *
   * @param sums Cumulative sums
   * @param begin Interval begin
   * @param end Interval end
   * @return Best change position
   */
  public static DoubleIntPair bestChangeInMean(double[] sums, int begin, int end) {
    final int len = end - begin, last = end - 1;
    final double suml = begin > 0 ? sums[begin - 1] : 0.;
    final double sumr = sums[last];

    int bestpos = begin;
    double bestscore = Double.NEGATIVE_INFINITY;
    // Iterate elements k=2..n-1 in math notation_
    for(int j = begin, km1 = 1; j < last; j++, km1++) {
      assert (km1 < len); // FIXME: remove eventually
      final double sumj = sums[j]; // Sum _inclusive_ j'th element.
      // Derive the left mean and right mean from the precomputed aggregates:
      final double lmean = (sumj - suml) / km1;
      final double rmean = (sumr - sumj) / (len - km1);
      // Equation 2.6.17 from the Basseville book
      final double dm = lmean - rmean;
      final double score = km1 * (double) (len - km1) * dm * dm;
      if(score > bestscore) {
        bestpos = j + 1;
        bestscore = score;
      }
    }
    return new DoubleIntPair(bestscore, bestpos);
  }

  /**
   * Fisher-Yates shuffle of a partial array
   *
   * @param bstrap Data to shuffle
   * @param len Length of valid data
   * @param rnd Random generator
   */
  public static void shuffle(double[] bstrap, int len, Random rnd) {
    int i = len;
    while(i > 0) {
      final int r = rnd.nextInt(i);
      --i;
      // Swap
      double tmp = bstrap[r];
      bstrap[r] = bstrap[i];
      bstrap[i] = tmp;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Number of samples for bootstrap significance.
     */
    public static final OptionID BOOTSTRAP_ID = new OptionID("changepointdetection.bootstrap.samples", //
        "Number of samples to draw for bootstrapping the confidence estimate.");

    /**
     * Mininum confidence.
     */
    public static final OptionID CONFIDENCE_ID = new OptionID("changepointdetection.bootstrap.confidence", //
        "Confidence level to use with bootstrap sampling.");

    /**
     * Random generator seed.
     */
    public static final OptionID RANDOM_ID = new OptionID("changepointdetection.seed", //
        "Random generator seed for bootstrap sampling.");

    /**
     * Number of samples for bootstrap significance.
     */
    int bootstrapSamples = 1000;

    /**
     * Mininum confidence.
     */
    double minConfidence;

    /**
     * Random generator
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter bootstrapsamplesP = new IntParameter(BOOTSTRAP_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setDefaultValue(1000);
      if(config.grab(bootstrapsamplesP)) {
        bootstrapSamples = bootstrapsamplesP.getValue();
      }
      DoubleParameter confidenceP = new DoubleParameter(CONFIDENCE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .setDefaultValue(1 - 2.5 / bootstrapSamples);
      if(config.grab(confidenceP)) {
        minConfidence = confidenceP.doubleValue();
      }

      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected OfflineChangePointDetectionAlgorithm makeInstance() {
      return new OfflineChangePointDetectionAlgorithm(minConfidence, bootstrapSamples, rnd);
    }
  }
}
