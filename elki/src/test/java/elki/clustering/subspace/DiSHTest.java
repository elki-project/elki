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
package elki.clustering.subspace;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.model.SubspaceModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

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
  @Test
  public void testDiSHResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-hierarchy.csv", 450);
    Clustering<SubspaceModel> result = new ELKIBuilder<>(DiSH.class) //
        .with(DiSH.Par.EPSILON_ID, 0.005) //
        .with(DiSH.Par.MU_ID, 50) //
        .build().autorun(db);
    assertFMeasure(db, result, .99516369);
    assertClusterSizes(result, new int[] { 50, 199, 201 });
  }

  @Test
  public void testDiSHSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-4-5d.ascii", 1100);
    Clustering<SubspaceModel> result = new ELKIBuilder<>(DiSH.class) //
        .with(DiSH.Par.EPSILON_ID, 0.1) //
        .with(DiSH.Par.MU_ID, 40) //
        .with(DiSH.Par.STRATEGY_ID, DiSH.Strategy.APRIORI) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.656432);
    assertClusterSizes(result, new int[] { 61, 84, 153, 187, 283, 332 });
  }
}
