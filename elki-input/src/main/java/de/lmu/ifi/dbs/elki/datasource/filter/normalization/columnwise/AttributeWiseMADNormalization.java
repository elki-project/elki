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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Median Absolute Deviation is used for scaling the data set as follows:
 *
 * First, the median, and median absolute deviation are computed in each axis.
 * Then, each value is projected to (x - median(X)) / MAD(X).
 *
 * This is similar to z-standardization of data sets, except that it is more
 * robust towards outliers, and only slightly more expensive to compute.
 *
 * @author Erich Schubert
 * @since 0.6.0
 * @param <V> vector type
 *
 * @assoc - - - NumberVector
 */
// TODO: extract superclass AbstractAttributeWiseNormalization
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseMADNormalization" })
public class AttributeWiseMADNormalization<V extends NumberVector> implements Normalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseMADNormalization.class);

  /**
   * Number vector factory.
   */
  protected NumberVector.Factory<V> factory;

  /**
   * Stores the median in each dimension.
   */
  private double[] median = new double[0];

  /**
   * Stores the inverse median absolute deviation in each dimension.
   */
  private double[] imadsigma = new double[0];

  /**
   * Constructor.
   */
  public AttributeWiseMADNormalization() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }
    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = (SimpleTypeInformation<?>) objects.meta(r);
      final List<?> column = (List<?>) objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      final List<V> castColumn = (List<V>) column;
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final VectorFieldTypeInformation<V> castType = (VectorFieldTypeInformation<V>) type;
      factory = FilterUtil.guessFactory(castType);

      // Scan to find the best
      final int dim = castType.getDimensionality();
      median = new double[dim];
      imadsigma = new double[dim];
      // Scratch space for testing:
      double[] test = new double[castColumn.size()];

      FiniteProgress dprog = LOG.isVerbose() ? new FiniteProgress("Analyzing data", dim, LOG) : null;
      // We iterate over dimensions, this kind of filter needs fast random
      // access.
      for(int d = 0; d < dim; d++) {
        for(int i = 0; i < test.length; i++) {
          test[i] = castColumn.get(i).doubleValue(d);
        }
        final double med = QuickSelect.median(test);
        median[d] = med;
        int zeros = 0;
        for(int i = 0; i < test.length; i++) {
          if((test[i] = Math.abs(test[i] - med)) == 0.) {
            zeros++;
          }
        }
        // Rescale the true MAD for the best standard deviation estimate:
        if(zeros < (test.length >>> 1)) {
          imadsigma[d] = NormalDistribution.PHIINV075 / QuickSelect.median(test);
        }
        else if(zeros == test.length) {
          LOG.warning("Constant attribute detected. Using MAD=1.");
          imadsigma[d] = 1.; // Does not matter. Constant distribution.
        }
        else {
          // We have more than 50% zeros, so the regular MAD estimate does not
          // work. Generalize the MAD approach to use the 50% non-zero value:
          final int rank = zeros + ((test.length - zeros) >> 1);
          final double rel = .5 + rank * .5 / test.length;
          imadsigma[d] = NormalDistribution.quantile(0., 1., rel) / QuickSelect.quickSelect(test, rank);
          LOG.warning("Near-constant attribute detected. Using modified MAD.");
        }
        LOG.incrementProcessed(dprog);
      }
      LOG.ensureCompleted(dprog);

      FiniteProgress nprog = LOG.isVerbose() ? new FiniteProgress("Data normalization", objects.dataLength(), LOG) : null;
      // Normalization scan
      double[] buf = new double[dim];
      for(int i = 0; i < objects.dataLength(); i++) {
        final V obj = castColumn.get(i);
        for(int d = 0; d < dim; d++) {
          buf[d] = normalize(d, obj.doubleValue(d));
        }
        castColumn.set(i, factory.newNumberVector(buf));
        LOG.incrementProcessed(nprog);
      }
      LOG.ensureCompleted(nprog);
    }
    return objects;
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    if(featureVector.getDimensionality() != median.length) {
      throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: " + featureVector.getDimensionality() + " former dimensionality: " + median.length);
    }
    double[] values = new double[featureVector.getDimensionality()];
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      values[d] = restore(d, featureVector.doubleValue(d));
    }
    return factory.newNumberVector(values);
  }

  /**
   * Normalize a single dimension.
   *
   * @param d Dimension
   * @param val Value
   * @return Normalized value
   */
  private double normalize(int d, double val) {
    return (val - median[d]) * imadsigma[d];
  }

  /**
   * Restore a single dimension.
   *
   * @param d Dimension
   * @param val Value
   * @return Normalized value
   */
  private double restore(int d, double val) {
    return (val / imadsigma[d]) + median[d];
  }

  @Override
  public String toString() {
    return new StringBuilder(1000).append("normalization class: ").append(getClass().getName()).append('\n') //
        .append("normalization median: ").append(FormatUtil.format(median)).append('\n') //
        .append("normalization scaling factor: ").append(FormatUtil.format(imadsigma)).toString();
  }
}
