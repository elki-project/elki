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
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a full PROCLUS run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that PROCLUS performance
 * doesn't unexpectedly drop on this data set (and also ensures that the
 * algorithms work, as a side effect).
 *
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PROCLUSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testPROCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<?> result = new ELKIBuilder<>(PROCLUS.class) //
        .with(PROCLUS.Par.L_ID, 1) //
        .with(PROCLUS.Par.K_ID, 4) //
        // NOTE: PROCLUS quality heavily depends on random...
        .with(PROCLUS.Par.SEED_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.83706);
    assertClusterSizes(result, new int[] { 38, 45, 200, 317 });
  }

  @Test
  public void testPROCLUSSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<?> result = new ELKIBuilder<>(PROCLUS.class) //
        .with(PROCLUS.Par.L_ID, 2) //
        .with(PROCLUS.Par.K_ID, 3) //
        // NOTE: PROCLUS quality heavily depends on random...
        .with(PROCLUS.Par.SEED_ID, 2) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.96489);
    assertClusterSizes(result, new int[] { 150, 290, 410 });
  }
}
