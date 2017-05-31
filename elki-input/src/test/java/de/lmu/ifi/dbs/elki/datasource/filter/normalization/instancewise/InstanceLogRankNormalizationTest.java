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
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the log-rank normalization filter.
 *
 * @author Matthew Arcifa
 */
public class InstanceLogRankNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    // Allow loading test data from resources.
    InstanceLogRankNormalization<DoubleVector> filter = ClassGenericsUtil.parameterizeOrAbort(InstanceLogRankNormalization.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    // This cast is now safe (vector field):
    int dim = ((FieldTypeInformation) bundle.meta(0)).getDimensionality();
    
    // TODO: Can you explain this, Erich?
    MeanVariance mvs = new MeanVariance();
    for(int ii = 0; ii < dim; ii++) {
      mvs.put(Math.log1p(ii / (double)(dim - 1 )) * MathUtil.ONE_BY_LOG2);
    }
    
    // Verify that each row has a min of 0, a max of 1, and that each row's mean and variance
    // is as calculated above. TODO: Why?
    MeanVarianceMinMax mms = new MeanVarianceMinMax();
    for(int row = 0; row < bundle.dataLength(); row++) {
      Object obj = bundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      mms.reset();
      for(int col = 0; col < dim; col++) {
        final double v = d.doubleValue(col);
        mms.put(v);
      }
      assertEquals("Min value is not as expected", 0., mms.getMin(), 1e-8);
      assertEquals("Max value is not as expected", 1., mms.getMax(), 1e-8);
      assertEquals("Mean value is not as expected", mvs.getMean(), mms.getMean(), 1e-8);
      assertEquals("Variance is not as expected", mvs.getNaiveVariance(), mms.getNaiveVariance(), 1e-8);
    }
  }
}