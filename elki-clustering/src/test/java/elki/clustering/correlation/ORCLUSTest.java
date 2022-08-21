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
package elki.clustering.correlation;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a full ORCLUS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that ORCLUS performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Elke Achtert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class ORCLUSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testORCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-hierarchy.csv", 450);
    Clustering<Model> result = new ELKIBuilder<>(ORCLUS.class) //
        .with(ORCLUS.Par.K_ID, 3) //
        .with(ORCLUS.Par.L_ID, 1) //
        .with(ORCLUS.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.6298);
    assertClusterSizes(result, new int[] { 22, 33, 395 });
  }

  @Test
  public void testORCLUSSkewedDisjoint() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-skewed-disjoint-3-5d.ascii", 601);
    Clustering<Model> result = new ELKIBuilder<>(ORCLUS.class) //
        .with(ORCLUS.Par.K_ID, 3) //
        .with(ORCLUS.Par.L_ID, 4) //
        .with(ORCLUS.Par.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.63327);
    assertClusterSizes(result, new int[] { 107, 152, 342 });
  }
}
