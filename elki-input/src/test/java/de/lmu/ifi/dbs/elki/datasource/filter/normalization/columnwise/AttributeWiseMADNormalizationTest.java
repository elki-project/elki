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

import java.util.Arrays;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the MAD normalization filter.
 *
 * @author //TODO: DO I NEED TO CHANGE THIS? 
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

    // Read the data from the file again, but don't filter it with MAD.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the unfiltered bundle was also read in correctly.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));
    assertEquals("Bundle of incorrect format", dim, ((FieldTypeInformation) bundle.meta(0)).getDimensionality());
    
    // Transpose the combined matrix formed by the DoubleVector objects in the unfiltered bundle.
    double[][] transpose = new double[dim][unfilteredBundle.dataLength()];
    for(int tCol = 0; tCol < unfilteredBundle.dataLength(); tCol++) {
      Object obj = unfilteredBundle.data(tCol, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int tRow = 0; tRow < dim; tRow++) {
        final double val = d.doubleValue(tRow);
        transpose[tRow][tCol] = val;
      }
    }
    
    // Sort each row of the transposed array.
    for(int row = 0; row < dim; row++) {
      Arrays.sort(transpose[row]);
    }
    
    // Calculate the median of each row of the transpose. of each column of the unfiltered bundle.
    double[] median = new double[dim];
    int mid = unfilteredBundle.dataLength() / 2;
    for(int row = 0; row < dim; row++) {
      median[row] = mid % 2 == 1 ? transpose[row][mid] : (transpose[row][mid - 1] + transpose[row][mid]) / 2.0;
    }
    
    // Calculate the MAD of each row of the transpose ie. of each column of the unfiltered bundle.
    double[] mad = new double[dim];
    for(int row = 0; row < dim; row++) {
      for(int col = 0; col < unfilteredBundle.dataLength(); col++) {
        transpose[row][col] = Math.abs(transpose[row][col] - median[row]);
      }
    }
    for(int row = 0; row < dim; row++) {
      Arrays.sort(transpose[row]);
      mad[row] = mid % 2 == 1 ? transpose[row][mid] : (transpose[row][mid - 1] + transpose[row][mid]) / 2.0;
    }

    //VERIFICATION
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object objFiltered = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objFiltered.getClass());
      Object objUnfiltered = unfilteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objUnfiltered.getClass());
      DoubleVector d_fil = (DoubleVector) objFiltered;
      DoubleVector d_unfil = (DoubleVector) objUnfiltered;
      for(int col = 0; col < dim; col++) {
        final double val_fil = d_fil.doubleValue(col);
        final double val_unfil = d_unfil.doubleValue(col);
        assertEquals("OH NO", val_fil, (val_unfil - median[col]) / mad[col], 1e-15);
      }
    }
  }
}
