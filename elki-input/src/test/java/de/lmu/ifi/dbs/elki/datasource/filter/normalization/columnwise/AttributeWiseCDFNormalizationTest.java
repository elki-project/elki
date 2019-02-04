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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMOMEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.UniformMinMaxEstimator;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the CDF normalization filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class AttributeWiseCDFNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normally-distributed-data-1.csv";
    AttributeWiseCDFNormalization<DoubleVector> filter = new ELKIBuilder<AttributeWiseCDFNormalization<DoubleVector>>(AttributeWiseCDFNormalization.class) //
        // Avoid cross-testing too many estimators.
        .with(AttributeWiseCDFNormalization.Parameterizer.DISTRIBUTIONS_ID, //
            Arrays.asList(NormalMOMEstimator.STATIC, UniformMinMaxEstimator.STATIC)) //
        .build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // We expect that approximately 25% of the values in each row are 0-0.25,
    // 25% in 0.25-0.5, 25% in 0.5-0.75, and 25% in 0.75-1 for each dimension
    int[][] counts = new int[dim][4];

    final int size = bundle.dataLength();
    for(int row = 0; row < size; row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        int q = (int) (val * 4);
        counts[col][q]++;
      }
    }
    for(int col = 0; col < dim; col++) {
      assertEquals("~25% of the values in each column should be between 0 and 0.25", .25, counts[col][0] / (double) size, .02);
      assertEquals("~25% of the values in each column should be between 0.25 and 0.5", .25, counts[col][1] / (double) size, .02);
      assertEquals("~25% of the values in each column should be between 0.5 and 0.75", .25, counts[col][2] / (double) size, .03);
      assertEquals("~25% of the values in each column should be between 0.5 and 0.75", .25, counts[col][3] / (double) size, .02);
    }
  }
}
