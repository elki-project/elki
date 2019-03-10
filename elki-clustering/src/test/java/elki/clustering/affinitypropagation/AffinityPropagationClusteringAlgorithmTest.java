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
package elki.clustering.affinitypropagation;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.MedoidModel;
import elki.database.Database;
import elki.distance.similarityfunction.kernel.PolynomialKernel;
import elki.utilities.ELKIBuilder;

/**
 * Test Affinity Propagation
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class AffinityPropagationClusteringAlgorithmTest extends AbstractClusterAlgorithmTest {
  /**
   * Run AffinityPropagationClusteringAlgorithm with fixed parameters and
   * compare the result to a golden standard.
   */
  @Test
  public void testAffinityPropagationClusteringAlgorithmResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<AffinityPropagationClusteringAlgorithm<DoubleVector>>(AffinityPropagationClusteringAlgorithm.class)//
        .build().run(db);
    testFMeasure(db, result, 0.957227259);
    testClusterSizes(result, new int[] { 5, 5, 7, 55, 105, 153 });
  }

  /**
   * Run AffinityPropagationClusteringAlgorithm with fixed parameters and
   * compare the result to a golden standard.
   */
  @Test
  public void testAffinityPropagationClusteringAlgorithmOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<MedoidModel> result = new ELKIBuilder<AffinityPropagationClusteringAlgorithm<DoubleVector>>(AffinityPropagationClusteringAlgorithm.class) //
        .build().run(db);
    testFMeasure(db, result, 0.351689882);
    testClusterSizes(result, new int[] { 24, 27, 29, 34, 36, 36, 37, 38, 41, 43, 43, 44, 46, 47, 56, 57 });
  }

  /**
   * Run AffinityPropagationClusteringAlgorithm with fixed parameters and
   * compare the result to a golden standard.
   */
  @Test
  public void testAffinityPropagationSimilarity() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<MedoidModel> result = new ELKIBuilder<AffinityPropagationClusteringAlgorithm<DoubleVector>>(AffinityPropagationClusteringAlgorithm.class) //
        .with(AffinityPropagationClusteringAlgorithm.Parameterizer.INITIALIZATION_ID, SimilarityBasedInitializationWithMedian.class) //
        .with(SimilarityBasedInitializationWithMedian.Parameterizer.SIMILARITY_ID, PolynomialKernel.class) //
        .build().run(db);
    testFMeasure(db, result, 0.352103);
    testClusterSizes(result, new int[] { 20, 30, 32, 33, 34, 35, 36, 39, 39, 40, 43, 45, 45, 49, 49, 69 });
  }
}
