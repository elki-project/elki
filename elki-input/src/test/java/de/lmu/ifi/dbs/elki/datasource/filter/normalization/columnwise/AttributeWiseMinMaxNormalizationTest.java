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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the min-max normalization filter.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class AttributeWiseMinMaxNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    AttributeWiseMinMaxNormalization<DoubleVector> filter = new ELKIBuilder<AttributeWiseMinMaxNormalization<DoubleVector>>(AttributeWiseMinMaxNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // We verify that minimum and maximum values in each column are 0 and 1:
    DoubleMinMax[] mms = DoubleMinMax.newArray(dim);
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        if(val > Double.NEGATIVE_INFINITY && val < Double.POSITIVE_INFINITY) {
          mms[col].put(val);
        }
      }
    }
    for(int col = 0; col < dim; col++) {
      assertEquals("Minimum not as expected", 0., mms[col].getMin(), 0.);
      assertEquals("Maximum not as expected", 1., mms[col].getMax(), 0.);
    }
  }

  /**
   * Test with default parameters and for correcting handling of NaN and Inf.
   */
  @Test
  public void testNaNParameters() {
    String filename = UNITTEST + "nan-test-1.csv";
    AttributeWiseMinMaxNormalization<DoubleVector> filter = new ELKIBuilder<AttributeWiseMinMaxNormalization<DoubleVector>>(AttributeWiseMinMaxNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();

    // We verify that minimum and maximum values in each column are 0 and 1:
    DoubleMinMax[] mms = DoubleMinMax.newArray(dim);
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        if(val > Double.NEGATIVE_INFINITY && val < Double.POSITIVE_INFINITY) {
          mms[col].put(val);
        }
      }
    }
    for(int col = 0; col < dim; col++) {
      assertEquals("Minimum not as expected", 0., mms[col].getMin(), 0.);
      assertEquals("Maximum not as expected", 1., mms[col].getMax(), 0.);
    }
  }
}
