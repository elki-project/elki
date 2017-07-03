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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the integer rank tie normalization filter.
 *
 * @author Matthew Arcifa
 */
public class IntegerRankTieNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    // Allow loading test data from resources.
    IntegerRankTieNormalization filter = ClassGenericsUtil.parameterizeOrAbort(IntegerRankTieNormalization.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();

    List<List<Integer>> arr = new ArrayList<List<Integer>>(dim);
    for(int col = 0; col < dim; col++) {
      arr.add(new ArrayList<Integer>());
    }
    
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", IntegerVector.class, obj.getClass());
      IntegerVector i = (IntegerVector) obj;
      for(int col = 0; col < dim; col++) {
        final int v = i.intValue(col);
        arr.get(col).add(v);
      }
    }
    
    /*
     * Verify that the smallest value is one less than its frequency.
     * 
     * Verify that the greatest value can be derived as a function of the size of the column
     * and the frequency of the greatest value.
     * 
     */
    for(int col = 0; col < dim; col++) {  
      final int min = Collections.min(arr.get(col));
      final int minFreq = Collections.frequency(arr.get(col), min);
      assertEquals("Unexpected min value", minFreq - 1, min);
      
      final int max = Collections.max(arr.get(col));
      final int maxFreq = Collections.frequency(arr.get(col), max);
      assertEquals("Unexpected max value", 2 * bundle.dataLength() - maxFreq - 1, max);
    }
  }
}
