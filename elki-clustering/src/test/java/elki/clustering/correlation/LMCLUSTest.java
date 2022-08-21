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
 * Test the LMCLUS algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LMCLUSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testLMCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);
    Clustering<Model> result = new ELKIBuilder<>(LMCLUS.class) //
        .with(LMCLUS.Par.MINSIZE_ID, 100) //
        .with(LMCLUS.Par.THRESHOLD_ID, 10) //
        .with(LMCLUS.Par.RANDOM_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.484274);
    assertClusterSizes(result, new int[] { 37, 563 });
  }

  @Test
  public void testLMCLUSOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
    Clustering<Model> result = new ELKIBuilder<>(LMCLUS.class) //
        .with(LMCLUS.Par.MINSIZE_ID, 100) //
        .with(LMCLUS.Par.THRESHOLD_ID, 10) //
        .with(LMCLUS.Par.RANDOM_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.921865);
    assertClusterSizes(result, new int[] { 200, 201, 249 });
  }
}
