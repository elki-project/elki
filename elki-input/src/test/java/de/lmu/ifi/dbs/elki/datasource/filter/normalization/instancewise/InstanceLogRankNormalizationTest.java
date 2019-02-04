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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the log-rank normalization filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class InstanceLogRankNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    InstanceLogRankNormalization<DoubleVector> filter = new ELKIBuilder<>(InstanceLogRankNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // Compute the expected mean and variances..
    MeanVariance expected = new MeanVariance();
    for(int ii = 0; ii < dim; ii++) {
      expected.put(Math.log1p(ii / (double) (dim - 1)) * MathUtil.ONE_BY_LOG2);
    }

    // The smallest value (except for ties) must be mapped to 0, the largest to
    // 1. And (again, except for ties), the mean and variance must match above
    // expected values of a uniform distribution.
    MeanVarianceMinMax mms = new MeanVarianceMinMax();
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        mms.put(d.doubleValue(col));
      }
      assertEquals("Min value is not 0", 0., mms.getMin(), 0);
      assertEquals("Max value is not 1", 1., mms.getMax(), 0);
      assertEquals("Mean value is not as expected", expected.getMean(), mms.getMean(), 1e-14);
      assertEquals("Variance is not as expected", expected.getNaiveVariance(), mms.getNaiveVariance(), 1e-14);
      mms.reset();
    }
  }
}
