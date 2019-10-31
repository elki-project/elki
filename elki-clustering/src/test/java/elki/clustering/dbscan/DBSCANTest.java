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
package elki.clustering.dbscan;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.Model;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

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
public class DBSCANTest extends AbstractClusterAlgorithmTest {
  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<DBSCAN<DoubleVector>>(DBSCAN.class) //
        .with(DBSCAN.Par.EPSILON_ID, 0.04) //
        .with(DBSCAN.Par.MINPTS_ID, 20) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.996413);
    assertClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<Model> result = new ELKIBuilder<DBSCAN<DoubleVector>>(DBSCAN.class) //
        .with(DBSCAN.Par.EPSILON_ID, 11.5) //
        .with(DBSCAN.Par.MINPTS_ID, 120) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.954382);
    assertClusterSizes(result, new int[] { 11, 200, 203, 224 });
  }
}