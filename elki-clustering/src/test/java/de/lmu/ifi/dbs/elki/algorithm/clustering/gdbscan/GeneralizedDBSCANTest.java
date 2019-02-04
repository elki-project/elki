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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.RadialBasisFunctionKernelFunction;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full DBSCAN run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that DBSCAN performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class GeneralizedDBSCANTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<>(GeneralizedDBSCAN.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 0.04) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 20) //
        .build().run(db);
    testFMeasure(db, result, 0.996413);
    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  @Test
  public void testDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<Model> result = new ELKIBuilder<>(GeneralizedDBSCAN.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 11.5) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 120) //
        .build().run(db);
    testFMeasure(db, result, 0.954382);
    testClusterSizes(result, new int[] { 11, 200, 203, 224 });
  }

  /**
   * Run Generalized DBSCAN with a similarity function.
   */
  @Test
  public void testSimilarityDBSCAN() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<>(GeneralizedDBSCAN.class) //
        .with(GeneralizedDBSCAN.Parameterizer.NEIGHBORHOODPRED_ID, SimilarityNeighborPredicate.class) //
        .with(SimilarityNeighborPredicate.Parameterizer.SIMILARITY_FUNCTION_ID, RadialBasisFunctionKernelFunction.class) //
        .with(SimilarityNeighborPredicate.Parameterizer.EPSILON_ID, 0.999) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 20) //
        .build().run(db);
    testFMeasure(db, result, 0.992897);
    testClusterSizes(result, new int[] { 28, 50, 102, 150 });
  }
}
