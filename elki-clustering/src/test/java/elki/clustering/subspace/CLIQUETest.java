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
package elki.clustering.subspace;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.model.SubspaceModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a full CLIQUE run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that CLIQUE performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class CLIQUETest extends AbstractClusterAlgorithmTest {
  /**
   * Run CLIQUE with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testCLIQUEResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<SubspaceModel> result = new ELKIBuilder<CLIQUE>(CLIQUE.class) //
        .with(CLIQUE.Par.TAU_ID, "0.1") //
        .with(CLIQUE.Par.XSI_ID, 20) //
        .build().run(db);
    // PairCounting is not appropriate here: overlapping clusterings!
    // testFMeasure(db, result, 0.9882);
    testClusterSizes(result, new int[] { 200, 200, 216, 400 });
  }

  /**
   * Run CLIQUE with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testCLIQUESubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<SubspaceModel> result = new ELKIBuilder<CLIQUE>(CLIQUE.class) //
        .with(CLIQUE.Par.TAU_ID, 0.2) //
        .with(CLIQUE.Par.XSI_ID, 6) //
        .build().run(db);
    // PairCounting is not appropriate here: overlapping clusterings!
    // testFMeasure(db, result, 0.433661);
    testClusterSizes(result, new int[] { 255, 409, 458, 458, 480 });
  }

  /**
   * Run CLIQUE with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testCLIQUESubspaceOverlappingPrune() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<SubspaceModel> result = new ELKIBuilder<CLIQUE>(CLIQUE.class) //
        .with(CLIQUE.Par.TAU_ID, 0.2) //
        .with(CLIQUE.Par.XSI_ID, 6) //
        .with(CLIQUE.Par.PRUNE_ID) //
        .build().run(db);
    // PairCounting is not appropriate here: overlapping clusterings!
    // testFMeasure(db, result, 0.433661);
    testClusterSizes(result, new int[] { 255, 409, 458, 458, 480 });
  }
}
