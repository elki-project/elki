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
 * Test FastDOC on a simple test data set.
 * <p>
 * On the first set, its an all-or-nothing depending on the parameters.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FastDOCTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testDOCSimple() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<?> result = new ELKIBuilder<>(FastDOC.class) //
        .with(DOC.Par.RANDOM_ID, 0) //
        .with(DOC.Par.ALPHA_ID, 0.4) //
        .with(DOC.Par.BETA_ID, 0.85) //
        .with(FastDOC.Par.D_ZERO_ID, 1) //
        .build().autorun(db);
    assertFMeasure(db, result, 1.0);
    assertClusterSizes(result, new int[] { 200, 400 });
  }

  @Test
  public void testDOCOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<?> result = new ELKIBuilder<>(FastDOC.class) //
        .with(DOC.Par.RANDOM_ID, 2) //
        .with(DOC.Par.ALPHA_ID, 0.4) //
        .with(DOC.Par.BETA_ID, 0.95) //
        .with(FastDOC.Par.D_ZERO_ID, 2) //
        .build().autorun(db);
    // Haven't found any working parameters for FastDOC on this data yet.
    assertFMeasure(db, result, .5477386);
    assertClusterSizes(result, new int[] { 850 });
  }
}
