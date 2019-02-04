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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.DiSHPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full DiSH run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that DiSH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DiSHTest extends AbstractClusterAlgorithmTest {
  /**
   * Run DiSH with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testDiSHResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-hierarchy.csv", 450);
    Clustering<SubspaceModel> result = new ELKIBuilder<DiSH<DoubleVector>>(DiSH.class) //
        .with(DiSH.Parameterizer.EPSILON_ID, 0.005) //
        .with(DiSH.Parameterizer.MU_ID, 50) //
        .build().run(db);
    testFMeasure(db, result, .99516369);
    testClusterSizes(result, new int[] { 50, 199, 201 });
  }

  /**
   * Run DiSH with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testDiSHSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-4-5d.ascii", 1100);
    Clustering<SubspaceModel> result = new ELKIBuilder<DiSH<DoubleVector>>(DiSH.class) //
        .with(DiSH.Parameterizer.EPSILON_ID, 0.1) //
        .with(DiSH.Parameterizer.MU_ID, 40) //
        .with(DiSHPreferenceVectorIndex.Factory.STRATEGY_ID, DiSHPreferenceVectorIndex.Strategy.APRIORI) //
        .build().run(db);
    testFMeasure(db, result, 0.656432);
    testClusterSizes(result, new int[] { 61, 84, 153, 187, 283, 332 });
  }
}
