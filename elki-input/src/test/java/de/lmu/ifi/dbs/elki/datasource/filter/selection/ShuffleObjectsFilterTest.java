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
package de.lmu.ifi.dbs.elki.datasource.filter.selection;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the shuffle objects filter.
 *
 * @author Matthew Arcifa
 */
public class ShuffleObjectsFilterTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void DefaultParameters() {
    String filename = UNITTEST + "sorted-data-1.csv";
    // Allow loading test data from resources.
    ShuffleObjectsFilter filter = ClassGenericsUtil.parameterizeOrAbort(ShuffleObjectsFilter.class, new ListParameterization());
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));
    assertEquals("Dimensionality 1 expected for input data", 1, ((FieldTypeInformation) filteredBundle.meta(0)).getDimensionality());
    assertEquals("Dimensionality 1 expected for input data", 1, ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality());
    
    // Verify that the elements of the unfiltered bundle are in sorted order.
    for(int row = 0; row < unfilteredBundle.dataLength() - 1; row++) {
      Object objFirst = unfilteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objFirst.getClass());
      DoubleVector dFirst = (DoubleVector) objFirst;
      final double vFirst = dFirst.doubleValue(0);
      Object objSecond = unfilteredBundle.data(row + 1, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objSecond.getClass());
      DoubleVector dSecond = (DoubleVector) objSecond;
      final double vSecond = dSecond.doubleValue(0);
      
      assertTrue("Values are expected to be in sorted order", vFirst <= vSecond);
    }
    
    // Verify that the elements of the filtered bundle are not in sorted order.
    // By verifying this, we can ascertain that the vectors have been shuffled.
    boolean isSorted = true;
    for(int row = 0; row < filteredBundle.dataLength() - 1; row++) {
      Object objFirst = filteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objFirst.getClass());
      DoubleVector dFirst = (DoubleVector) objFirst;
      final double vFirst = dFirst.doubleValue(0);
      Object objSecond = filteredBundle.data(row + 1, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objSecond.getClass());
      DoubleVector dSecond = (DoubleVector) objSecond;
      final double vSecond = dSecond.doubleValue(0);
      
      if(vFirst > vSecond) {
        isSorted = false;
        break;
      }
    }
    
    assertFalse("Elements are not shuffled", isSorted);
  }
}
