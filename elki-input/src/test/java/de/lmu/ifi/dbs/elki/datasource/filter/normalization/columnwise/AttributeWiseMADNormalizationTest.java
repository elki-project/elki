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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the MAD normalization filter.
 *
 * @author Matthew Arcifa
 */
public class AttributeWiseMADNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    // Allow loading test data from resources.
    AttributeWiseMADNormalization<DoubleVector> filter = ClassGenericsUtil.parameterizeOrAbort(AttributeWiseMADNormalization.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();
    
    // Count how many values in each column are positive, how many are negative, and how many are greater than 1
    // or less than -1.
    int[] countNotPositive = new int[dim];
    int[] countPositive = new int[dim];
    int[] countAbsGreaterOne = new int[dim];
    
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        if(val > 0.0){
          countPositive[col]++;
        } else {
          countNotPositive[col]++;
        }
        if(Math.abs(val) >= NormalDistribution.PHIINV075){
          countAbsGreaterOne[col]++;
        }
      }
    }

    // Verify that ~50% of the values in each column are negative (=> ~50% of the values are positive).
    // Verify that ~50% of the values are either greater than 1 or less than -1.
    for(int col = 0; col < dim; col++) {
      assertEquals("~50% of the values in each column should be positive", .5, (double)countPositive[col] / (double)bundle.dataLength(), 0.);
      assertEquals("~50% of the values in each column should be > 1 or < -1", .5, (double)countAbsGreaterOne[col] / (double)bundle.dataLength(), 0.);
    }
  }
}
