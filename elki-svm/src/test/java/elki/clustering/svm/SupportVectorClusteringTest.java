/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
 * Tests for Support Vector Clustering ({@link SupportVectorClustering})
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class SupportVectorClusteringTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSigma() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<>(SupportVectorClustering.class) //
        .with(SupportVectorClustering.Par.KERNEL_ID, RadialBasisFunctionKernel.class) //
        .with(SupportVectorClustering.Par.C_ID, 0.05) //
        .with(RadialBasisFunctionKernel.Par.SIGMA_ID, 0.1) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.945171);
    assertClusterSizes(result, new int[] { 22, 57, 101, 150 });
  }

  @Test
  public void testGamma() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<>(SupportVectorClustering.class) //
        .with(SupportVectorClustering.Par.KERNEL_ID, RadialBasisFunctionKernel.class) //
        .with(SupportVectorClustering.Par.C_ID, 0.05) //
        .with(RadialBasisFunctionKernel.Par.GAMMA_ID, 50) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.945171);
    assertClusterSizes(result, new int[] { 22, 57, 101, 150 });
  }
}
