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
package de.lmu.ifi.dbs.elki.datasource.cleaning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.cleaning.DropNaNFilter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the NaN-drop cleaning filter.
 *
 * @author Matthew Arcifa
 */
public class DropNaNFilterTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "nan-test-1.csv";
    // Allow loading test data from resources.
    DropNaNFilter filter = ClassGenericsUtil.parameterizeOrAbort(DropNaNFilter.class, new ListParameterization());
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));
    // This cast is now safe (vector field):
    int dimFiltered = ((FieldTypeInformation) filteredBundle.meta(0)).getDimensionality();    
    int dimUnfiltered = ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality();
    
    // Ensure that at least a single NaN exists in the unfiltered bundle.
    boolean NaNfound = false;
    for(int row = 0; row < unfilteredBundle.dataLength(); row++) {
      Object obj = unfilteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dimUnfiltered; col++) {
        final double v = d.doubleValue(col);
        if(Double.isNaN(v)) {
          NaNfound = true;
          break; //Forgive me, Lord, for I have sinned.
        }
      }
    }
    assertTrue("NaN expected in unfiltered data", NaNfound);
    
    // Ensure that no single NaN exists in the filtered bundle.
    for(int row = 0; row < filteredBundle.dataLength(); row++) {
      Object obj = filteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dimFiltered; col++) {
        final double v = d.doubleValue(col);
        assertTrue("NaN not expected", !Double.isNaN(v));
      }
    }
  }
}
