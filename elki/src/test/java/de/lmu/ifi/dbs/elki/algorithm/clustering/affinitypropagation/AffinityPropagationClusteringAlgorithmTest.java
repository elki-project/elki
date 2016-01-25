package de.lmu.ifi.dbs.elki.algorithm.clustering.affinitypropagation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test Affinity Propagation
 *
 * @author Erich Schubert
 * @since 0.7.1
 */
public class AffinityPropagationClusteringAlgorithmTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run AffinityPropagationClusteringAlgorithm with fixed parameters and
   * compare the result to a golden standard.
   *
   * @throws ParameterException
   */
  @Test
  public void testAffinityPropagationClusteringAlgorithmResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    AffinityPropagationClusteringAlgorithm<DoubleVector> apc = ClassGenericsUtil.parameterizeOrAbort(AffinityPropagationClusteringAlgorithm.class, params);
    testParameterizationOk(params);

    // run AffinityPropagationClusteringAlgorithm on database
    Clustering<MedoidModel> result = apc.run(db);

    testFMeasure(db, result, 0.957227259);
    testClusterSizes(result, new int[] { 5, 5, 7, 55, 105, 153 });
  }

  /**
   * Run AffinityPropagationClusteringAlgorithm with fixed parameters and
   * compare the result to a golden standard.
   *
   * @throws ParameterException
   */
  @Test
  public void testAffinityPropagationClusteringAlgorithmOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    AffinityPropagationClusteringAlgorithm<DoubleVector> apc = ClassGenericsUtil.parameterizeOrAbort(AffinityPropagationClusteringAlgorithm.class, params);
    testParameterizationOk(params);

    // run AffinityPropagationClusteringAlgorithm on database
    Clustering<MedoidModel> result = apc.run(db);
    testFMeasure(db, result, 0.351689882);
    testClusterSizes(result, new int[] { 24, 27, 29, 34, 36, 36, 37, 38, 41, 43, 43, 44, 46, 47, 56, 57 });
  }

  /**
   * Run AffinityPropagationClusteringAlgorithm with fixed parameters and
   * compare the result to a golden standard.
   *
   * @throws ParameterException
   */
  @Test
  public void testAffinityPropagationSimilarity() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(AffinityPropagationClusteringAlgorithm.Parameterizer.INITIALIZATION_ID, SimilarityBasedInitializationWithMedian.class);
    params.addParameter(SimilarityBasedInitializationWithMedian.Parameterizer.SIMILARITY_ID, PolynomialKernelFunction.class);
    AffinityPropagationClusteringAlgorithm<DoubleVector> apc = ClassGenericsUtil.parameterizeOrAbort(AffinityPropagationClusteringAlgorithm.class, params);
    testParameterizationOk(params);

    // run AffinityPropagationClusteringAlgorithm on database
    Clustering<MedoidModel> result = apc.run(db);
    testFMeasure(db, result, 0.352103);
    testClusterSizes(result, new int[] { 20, 30, 32, 33, 34, 35, 36, 39, 39, 40, 43, 45, 45, 49, 49, 69 });
  }
}