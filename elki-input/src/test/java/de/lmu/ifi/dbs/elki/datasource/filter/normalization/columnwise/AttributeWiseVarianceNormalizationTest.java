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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the variance-max normalization filter.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class AttributeWiseVarianceNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    AttributeWiseVarianceNormalization<DoubleVector> filter = new ELKIBuilder<AttributeWiseVarianceNormalization<DoubleVector>>(AttributeWiseVarianceNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // We verify that the resulting data has mean 0 and variance 1 in each
    // column:
    MeanVariance[] mvs = MeanVariance.newArray(dim);
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double v = d.doubleValue(col);
        if(v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY) {
          mvs[col].put(v);
        }
      }
    }
    for(int col = 0; col < dim; col++) {
      assertEquals("Mean not as expected", 0., mvs[col].getMean(), 1e-14);
      assertEquals("Variance not as expected", 1., mvs[col].getNaiveVariance(), 1e-14);
    }
  }

  /**
   * Test with default parameters and for correcting handling of NaN and Inf.
   */
  @Test
  public void testNaNParameters() {
    String filename = UNITTEST + "nan-test-1.csv";
    AttributeWiseVarianceNormalization<DoubleVector> filter = new ELKIBuilder<AttributeWiseVarianceNormalization<DoubleVector>>(AttributeWiseVarianceNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();

    // Verify that the resulting data has mean 0 and variance 1 in each column:
    MeanVariance[] mvs = MeanVariance.newArray(dim);
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double v = d.doubleValue(col);
        if(v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY) {
          mvs[col].put(v);
        }
      }
    }
    for(int col = 0; col < dim; col++) {
      assertEquals("Mean not as expected", 0., mvs[col].getMean(), 1e-15);
      assertEquals("Variance not as expected", 1., mvs[col].getNaiveVariance(), 1e-15);
    }
  }
}
