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
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the Erf normalization filter.
 *
 * @author Matthew Arcifa
 */
public class AttributeWiseErfNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normally-distributed-data-1.csv";
    // Allow loading test data from resources.
    AttributeWiseVarianceNormalization<DoubleVector> varianceFilter = ClassGenericsUtil.parameterizeOrAbort(AttributeWiseVarianceNormalization.class, new ListParameterization());
    AttributeWiseErfNormalization<DoubleVector> erfFilter = ClassGenericsUtil.parameterizeOrAbort(AttributeWiseErfNormalization.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, varianceFilter, erfFilter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();
    
    // We calculate the mean and standard deviation of each column.
    MeanVariance[] mvs = MeanVariance.newArray(dim);
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dim; col++) {
        final double v = d.doubleValue(col);
        if(v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY) {
          mvs[col].put(v);
        }
      }
    }
    
    // Count how many values in each column are within one standard deviation of the mean.
    int[] countWithinOneStdDev = new int[dim];
    
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        if(Math.abs(val - mvs[col].getMean()) <= Math.sqrt(mvs[col].getSampleVariance())){
          countWithinOneStdDev[col]++;
        }
      }
    }

    // Verify that ~68% of the values in each column are within one standard deviation of the mean.
    for(int col = 0; col < dim; col++) {
      assertEquals("~68% of the values in each column should be within 1 standard deviation of the mean", .68, (double)countWithinOneStdDev[col] / (double)bundle.dataLength(), .2);
    }
  }
}
