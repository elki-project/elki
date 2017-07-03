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
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SingularValueDecomposition;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the classic MDS transformation filter.
 *
 * @author Matthew Arcifa
 */
public class ClassicMultidimensionalScalingTransformTest extends AbstractDataSourceTest {
  /**
   * Test with parameters.
   */
  @Test
  public void parameters() {
    int pdim = 2;
    String filename = UNITTEST + "transformation-test-1.csv";
    // Allow loading test data from resources.
    ListParameterization config = new ListParameterization();
    config.addParameter(ClassicMultidimensionalScalingTransform.Parameterizer.DIM_ID, pdim);
    config.addParameter(ClassicMultidimensionalScalingTransform.Parameterizer.DISTANCE_ID, EuclideanDistanceFunction.class);
    ClassicMultidimensionalScalingTransform<DoubleVector, DoubleVector> filter = ClassGenericsUtil.parameterizeOrAbort(ClassicMultidimensionalScalingTransform.class, config);
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));
    // This cast is now safe (vector field):
    assertEquals("Test file interpreted incorrectly", ((FieldTypeInformation) filteredBundle.meta(0)).getDimensionality(), ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality());
    // Verify that the filtered and unfiltered bundles have the same length.
    assertEquals("Test file interpreted incorrectly", filteredBundle.dataLength(), unfilteredBundle.dataLength());
    int dim = ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality();
    
    // Caution: this verification is naturally O(n^2), don't test with too much test data.
    // Verify that the euclidean distance between any two points is identical before and after the MDS transform is performed.
    
    //Calculate the covariance matricies of the filtered and unfiltered bundles.
    CovarianceMatrix cmUnfil = new CovarianceMatrix(dim);
    CovarianceMatrix cmFil = new CovarianceMatrix(dim);
    
    for(int outer = 0; outer < filteredBundle.dataLength(); outer++) {
      Object objFiltered_1 = filteredBundle.data(outer, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objFiltered_1.getClass());
      Object objUnfiltered_1 = unfilteredBundle.data(outer, 0);
      assertEquals("Unexpected data type", DoubleVector.class, objUnfiltered_1.getClass());
      DoubleVector dFil_1 = (DoubleVector) objFiltered_1;
      DoubleVector dUnfil_1 = (DoubleVector) objUnfiltered_1;
      cmUnfil.put(dUnfil_1);
      cmFil.put(dFil_1);
      
      for(int row = 0; row < filteredBundle.dataLength(); row++) {
        Object objFiltered_2 = filteredBundle.data(row, 0);
        assertEquals("Unexpected data type", DoubleVector.class, objFiltered_2.getClass());
        Object objUnfiltered_2 = unfilteredBundle.data(row, 0);
        assertEquals("Unexpected data type", DoubleVector.class, objUnfiltered_2.getClass());
        DoubleVector dFil_2 = (DoubleVector) objFiltered_2;
        DoubleVector dUnfil_2 = (DoubleVector) objUnfiltered_2;
        assertEquals("Expected same distance", EuclideanDistanceFunction.STATIC.distance(dFil_1, dFil_2), EuclideanDistanceFunction.STATIC.distance(dUnfil_1, dUnfil_2), 1e-8);
      }
    }
    
    // Calculate the SVD of the covariance matrix of the unfiltered data.
    // Verify that this SVD represents the diagonals of the covariance matrix of the filtered data.
    
    double[][] ncmUnfil = cmUnfil.destroyToPopulationMatrix();
    double[][] ncmFil = cmFil.destroyToPopulationMatrix();
    
    SingularValueDecomposition svd = new SingularValueDecomposition(ncmUnfil);
    double[] dia = svd.getSingularValues();
    
    for(int ii = 0; ii < dia.length; ii++) {
      assertEquals("Unexpected covariance", dia[ii], ncmFil[ii][ii], 1e-8);
    }
  }
}
