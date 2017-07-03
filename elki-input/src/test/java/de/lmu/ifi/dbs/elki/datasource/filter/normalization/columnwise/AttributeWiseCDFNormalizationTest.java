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
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the CDF normalization filter.
 *
 * @author Matthew Arcifa
 */
public class AttributeWiseCDFNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normally-distributed-data-1.csv";
    // Allow loading test data from resources.
    AttributeWiseCDFNormalization<DoubleVector> filter = ClassGenericsUtil.parameterizeOrAbort(AttributeWiseCDFNormalization.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();

    // We expect that approximately 25% of the values in each row are 0 - 0.25,
    // 25% between 0.25 and 0.5, 25% between 0.5 and 0.75, and 25% between 0.75 and 1.
    
    int[] countFirstQuarter = new int[dim];
    int[] countSecondQuarter = new int[dim];
    int[] countThirdQuarter = new int[dim];
    
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        if(val > Double.NEGATIVE_INFINITY && val < Double.POSITIVE_INFINITY) {
          if(val <= .5) {
            if(val <= .25) {
              countFirstQuarter[col]++;
            } else {
              countSecondQuarter[col]++;
            }
          } else {
            if(val <= .75) {
              countThirdQuarter[col]++;
            }
          }
        }
      }
    }
    for(int col = 0; col < dim; col++) {
      assertEquals("~25% of the values in each column should be between 0 and 0.25", .25, (double)countFirstQuarter[col] / (double)bundle.dataLength(), .1);
      assertEquals("~25% of the values in each column should be between 0.25 and 0.5", .25, (double)countSecondQuarter[col] / (double)bundle.dataLength(), .1);
      assertEquals("~25% of the values in each column should be between 0.5 and 0.75", .25, (double)countThirdQuarter[col] / (double)bundle.dataLength(), .1);
    }
  }
}
