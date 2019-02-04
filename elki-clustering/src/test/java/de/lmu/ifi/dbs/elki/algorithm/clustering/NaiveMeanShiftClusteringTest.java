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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.*;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for canopy clustering.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class NaiveMeanShiftClusteringTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testTriweight() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<NaiveMeanShiftClustering<DoubleVector>>(NaiveMeanShiftClustering.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.KERNEL_ID, TriweightKernelDensityFunction.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.RANGE_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.960385);
    testClusterSizes(result, new int[] { 1, 1, 1, 2, 3, 3, 3, 4, 55, 105, 152 });
  }

  @Test
  public void testTricube() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<NaiveMeanShiftClustering<DoubleVector>>(NaiveMeanShiftClustering.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.KERNEL_ID, TricubeKernelDensityFunction.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.RANGE_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.9482767);
    testClusterSizes(result, new int[] { 1, 1, 2, 2, 3, 4, 55, 110, 152 });
  }

  @Test
  public void testUniform() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<NaiveMeanShiftClustering<DoubleVector>>(NaiveMeanShiftClustering.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.KERNEL_ID, UniformKernelDensityFunction.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.RANGE_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.943331);
    testClusterSizes(result, new int[] { 1, 1, 1, 1, 3, 4, 56, 110, 153 });
  }

  @Test
  public void testCosine() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<NaiveMeanShiftClustering<DoubleVector>>(NaiveMeanShiftClustering.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.KERNEL_ID, CosineKernelDensityFunction.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.RANGE_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.9352097);
    testClusterSizes(result, new int[] { 2, 2, 4, 56, 112, 154 });
  }

  @Test
  public void testTriangular() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<NaiveMeanShiftClustering<DoubleVector>>(NaiveMeanShiftClustering.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.KERNEL_ID, TriangularKernelDensityFunction.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.RANGE_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.935209);
    testClusterSizes(result, new int[] { 2, 2, 4, 56, 112, 154 });
  }

  @Test
  public void testNaiveMeanShiftClusteringOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> result = new ELKIBuilder<NaiveMeanShiftClustering<DoubleVector>>(NaiveMeanShiftClustering.class) //
        .with(NaiveMeanShiftClustering.Parameterizer.RANGE_ID, 25) //
        .build().run(db);
    testFMeasure(db, result, 0.9385142);
    testClusterSizes(result, new int[] { 202, 209, 227 });
  }
}
