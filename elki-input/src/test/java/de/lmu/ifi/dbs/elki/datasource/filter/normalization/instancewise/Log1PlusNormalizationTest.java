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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

import net.jafama.FastMath;

/**
 * Test the log 1 plus normalization filter.
 *
 * @author Matthew Arcifa
 */
public class Log1PlusNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    // Allow loading test data from resources.
    InstanceMinMaxNormalization<DoubleVector> minMaxFilter = ClassGenericsUtil.parameterizeOrAbort(InstanceMinMaxNormalization.class, new ListParameterization());
    Log1PlusNormalization<DoubleVector> log1plusFilter = ClassGenericsUtil.parameterizeOrAbort(Log1PlusNormalization.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, minMaxFilter, log1plusFilter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();
    
    // We verify that minimum and maximum values in each row are 0 and 1:
    DoubleMinMax mms = new DoubleMinMax();
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      mms.reset();
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        if(val > Double.NEGATIVE_INFINITY && val < Double.POSITIVE_INFINITY) {
          mms.put(val);
        }
      }
      assertEquals("Minimum not expected", 0., mms.getMin(), 1e-15);
      assertEquals("Maximum not expected", 1., mms.getMax(), 1e-15);
    }
  }
  
  /**
   * Test with non-default parameters to ensure that both branches of the filter are tested.
   */
  @Test
  public void parameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    // Allow loading test data from resources.
    // Use the value of b as the boost value.
    double b = 15.;
    ListParameterization config = new ListParameterization();
    config.addParameter(Log1PlusNormalization.Parameterizer.BOOST_ID, b);
    Log1PlusNormalization<DoubleVector> filter = ClassGenericsUtil.parameterizeOrAbort(Log1PlusNormalization.class, config);
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));
    // This cast is now safe (vector field):
    assertEquals("Test file interpreted incorrectly", ((FieldTypeInformation) filteredBundle.meta(0)).getDimensionality(), ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality());
    int dim = ((FieldTypeInformation) filteredBundle.meta(0)).getDimensionality();
    // Verify that the filtered and unfiltered bundles have the same length.
    assertEquals("Test file interpreted incorrectly", filteredBundle.dataLength(), unfilteredBundle.dataLength());

    // Verify that the filter correctly applies the specified mathematical method.
    for(int row = 0; row < filteredBundle.dataLength(); row++) {
      Object objFiltered = filteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objFiltered.getClass());
      Object objUnfiltered = unfilteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objUnfiltered.getClass());
      DoubleVector dFil = (DoubleVector) objFiltered;
      DoubleVector dUnfil = (DoubleVector) objUnfiltered;      
      for(int col = 0; col < dim; col++) {
        final double vFil = dFil.doubleValue(col);
        final double vUnfil = dUnfil.doubleValue(col);
        assertEquals("Value not as expected", vFil, FastMath.log1p(Math.abs(vUnfil) * b) / FastMath.log1p(b), 1e-15);
      }
    }
  }
}