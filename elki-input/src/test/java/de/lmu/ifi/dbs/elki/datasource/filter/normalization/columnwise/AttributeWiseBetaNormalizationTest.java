/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.BetaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.NormalMOMEstimator;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the Beta normalization filter.
 *
 * @author Matthew Arcifa
 */
public class AttributeWiseBetaNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with parameter p as alpha.
   */
  @Test
  public void parameters() {
    final double p = .88;
    String filename = UNITTEST + "normally-distributed-data-1.csv";
    // Allow loading test data from resources.
    ListParameterization config = new ListParameterization();
    config.addParameter(AttributeWiseBetaNormalization.Parameterizer.ALPHA_ID, p);
    // Avoid cross-testing too many estimators.
    config.addParameter(AttributeWiseBetaNormalization.Parameterizer.DISTRIBUTIONS_ID, Arrays.asList(NormalMOMEstimator.STATIC));
    AttributeWiseBetaNormalization<DoubleVector> filter = ClassGenericsUtil.parameterizeOrAbort(AttributeWiseBetaNormalization.class, config);
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    BetaDistribution dist = new BetaDistribution(p, p);
    final double quantile = dist.quantile(p);

    // Verify that p% of the values in each column are less than the quantile.
    int[] countUnderQuantile = new int[dim];

    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double v = d.doubleValue(col);
        if(v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY) {
          if(v < quantile) {
            countUnderQuantile[col]++;
          }
        }
      }
    }

    for(int col = 0; col < dim; col++) {
      double actual = countUnderQuantile[col] / (double) bundle.dataLength();
      assertEquals("p% of the values should be under the quantile", p, actual, .05);
    }
  }
}
