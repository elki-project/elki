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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the PCA transformation filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class GlobalPrincipalComponentAnalysisTransformTest extends AbstractDataSourceTest {
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "transformation-test-1.csv";
    GlobalPrincipalComponentAnalysisTransform<DoubleVector> filter = new ELKIBuilder<GlobalPrincipalComponentAnalysisTransform<DoubleVector>>(GlobalPrincipalComponentAnalysisTransform.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // We verify that the result has mean 0 and variance 1 in each column.
    // We also expect that covariances of any two columns are 0.
    CovarianceMatrix cm = new CovarianceMatrix(dim);
    MeanVariance[] mvs = MeanVariance.newArray(dim);
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      cm.put(d);
      for(int col = 0; col < dim; col++) {
        mvs[col].put(d.doubleValue(col));
      }
    }
    double[][] ncm = cm.destroyToPopulationMatrix();
    for(int col = 0; col < dim; col++) {
      for(int row = 0; row < dim; row++) {
        assertEquals("Unexpected covariance", col == row ? 1. : 0., ncm[row][col], 1e-15);
      }
      assertEquals("Mean not as expected", 0., mvs[col].getMean(), 1e-15);
      assertEquals("Variance not as expected", 1., mvs[col].getNaiveVariance(), 1e-15);
    }
  }

  @Test
  public void rotateOnly() {
    String filename = UNITTEST + "transformation-test-1.csv";
    GlobalPrincipalComponentAnalysisTransform<DoubleVector> filter = new ELKIBuilder<GlobalPrincipalComponentAnalysisTransform<DoubleVector>>(GlobalPrincipalComponentAnalysisTransform.class) //
        .with(GlobalPrincipalComponentAnalysisTransform.Parameterizer.MODE_ID, GlobalPrincipalComponentAnalysisTransform.Mode.CENTER_ROTATE).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // We verify that the result has mean 0.
    // We also expect that covariances of any two columns are 0.
    CovarianceMatrix cm = new CovarianceMatrix(dim);
    double[] mvs = new double[dim];
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      cm.put(d);
      for(int col = 0; col < dim; col++) {
        mvs[col] += d.doubleValue(col);
      }
    }
    VMath.timesEquals(mvs, 1. / bundle.dataLength());
    double[][] ncm = cm.destroyToPopulationMatrix();
    for(int col = 0; col < dim; col++) {
      for(int row = 0; row < dim; row++) {
        if(row == col) {
          assertTrue("Unexpected variance", ncm[row][col] > 10);
        }
        else {
          assertEquals("Unexpected covariance", 0., ncm[row][col], 1e-12);
        }
      }
      assertEquals("Mean not as expected", 0., mvs[col], 1e-13);
    }
  }
}
