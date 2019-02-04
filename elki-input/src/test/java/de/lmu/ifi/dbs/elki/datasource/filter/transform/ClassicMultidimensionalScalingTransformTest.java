/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SingularValueDecomposition;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the classic MDS transformation filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class ClassicMultidimensionalScalingTransformTest extends AbstractDataSourceTest {
  /**
   * Test with parameters.
   */
  @Test
  public void parameters() {
    int pdim = 2;
    String filename = UNITTEST + "transformation-test-1.csv";
    ClassicMultidimensionalScalingTransform<DoubleVector, DoubleVector> filter = new ELKIBuilder<ClassicMultidimensionalScalingTransform<DoubleVector, DoubleVector>>(ClassicMultidimensionalScalingTransform.class) //
        .with(ClassicMultidimensionalScalingTransform.Parameterizer.DIM_ID, pdim) //
        .with(ClassicMultidimensionalScalingTransform.Parameterizer.DISTANCE_ID, EuclideanDistanceFunction.class) //
        .build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    int dimu = getFieldDimensionality(unfilteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);
    int dimf = getFieldDimensionality(filteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);
    assertEquals("Dimensionality not as requested", pdim, dimf);

    // Verify that the Euclidean distance between any two points is identical
    // before and after the MDS transform is performed - O(n^2)!

    // Calculate the covariance matricies of the filtered and unfiltered
    // bundles.
    CovarianceMatrix cmUnfil = new CovarianceMatrix(dimu);
    CovarianceMatrix cmFil = new CovarianceMatrix(dimf);

    for(int outer = 0; outer < filteredBundle.dataLength(); outer++) {
      DoubleVector dFil_1 = get(filteredBundle, outer, 0, DoubleVector.class);
      DoubleVector dUnfil_1 = get(unfilteredBundle, outer, 0, DoubleVector.class);
      cmUnfil.put(dUnfil_1);
      cmFil.put(dFil_1);

      for(int row = outer + 1; row < filteredBundle.dataLength(); row++) {
        DoubleVector dFil_2 = get(filteredBundle, row, 0, DoubleVector.class);
        DoubleVector dUnfil_2 = get(unfilteredBundle, row, 0, DoubleVector.class);
        final double distF = EuclideanDistanceFunction.STATIC.distance(dFil_1, dFil_2);
        final double distU = EuclideanDistanceFunction.STATIC.distance(dUnfil_1, dUnfil_2);
        assertEquals("Expected same distance", distU, distF, 1e-11);
      }
    }

    // Calculate the SVD of the covariance matrix of the unfiltered data.
    // Verify that this SVD represents the diagonals of the covariance matrix of
    // the filtered data.

    double[][] ncmUnfil = cmUnfil.destroyToPopulationMatrix();
    double[][] ncmFil = cmFil.destroyToPopulationMatrix();

    SingularValueDecomposition svd = new SingularValueDecomposition(ncmUnfil);
    double[] dia = svd.getSingularValues();

    for(int ii = 0; ii < dia.length; ii++) {
      assertEquals("Unexpected covariance", dia[ii], ncmFil[ii][ii], 1e-11);
    }
  }
}
