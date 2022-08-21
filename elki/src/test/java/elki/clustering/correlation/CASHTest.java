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
 * Perform a full CASH run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that CASH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class CASHTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testCASHResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);
    Clustering<Model> result = new ELKIBuilder<>(CASH.class) //
        .with(CASH.Par.JITTER_ID, 0.7) //
        .with(CASH.Par.MINPTS_ID, 50) //
        .with(CASH.Par.MAXLEVEL_ID, 25) //
        .with(CASH.Par.ADJUST_ID) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.50074); // with hierarchical pairs: 0.64102
    assertClusterSizes(result, new int[] { 18, 80, 252, 468 });
  }

  @Test
  public void testCASHEmbedded() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-embedded-2-4d.ascii", 600);
    Clustering<Model> result = new ELKIBuilder<>(CASH.class) //
        .with(CASH.Par.JITTER_ID, 0.7) //
        .with(CASH.Par.MINPTS_ID, 160) //
        .with(CASH.Par.MAXLEVEL_ID, 40) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.443246);
    assertClusterSizes(result, new int[] { 169, 196, 235 });
  }
}
