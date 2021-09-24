/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.clustering.svm;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.database.Database;
import elki.similarity.kernel.RadialBasisFunctionKernel;
import elki.utilities.ELKIBuilder;

/**
 * Tests for Support Vector Clustering ({@link SVC})
 * 
 * @author Robert Gehde
 *
 */
public class SVCTest extends AbstractClusterAlgorithmTest{

  @Test
  public void testSigma() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<>(SVC.class) //
        .with(SVC.Par.KERNEL_ID, RadialBasisFunctionKernel.class) //
        .with(SVC.Par.C_ID, 0.05) //
        .with(RadialBasisFunctionKernel.Par.SIGMA_ID, 0.1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.944456);
    assertClusterSizes(result, new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 2, 60, 106, 154 });
  }

  @Test
  public void testGamma() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<>(SVC.class) //
        .with(SVC.Par.KERNEL_ID, RadialBasisFunctionKernel.class) //
        .with(SVC.Par.C_ID, 0.05) //
        .with(RadialBasisFunctionKernel.Par.GAMMA_ID, 50) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.944456);
    assertClusterSizes(result, new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 2, 60, 106, 154 });
  }

}
