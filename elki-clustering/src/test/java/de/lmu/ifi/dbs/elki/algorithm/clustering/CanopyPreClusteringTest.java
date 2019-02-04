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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for canopy clustering.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CanopyPreClusteringTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testCanopyPreClusteringResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<CanopyPreClustering<DoubleVector>>(CanopyPreClustering.class) //
        .with(CanopyPreClustering.Parameterizer.T1_ID, 0.2) //
        .with(CanopyPreClustering.Parameterizer.T2_ID, 0.2) //
        .build().run(db);
    testFMeasure(db, result, 0.96691368);
    testClusterSizes(result, new int[] { 1, 1, 1, 1, 1, 1, 1, 2, 2, 4, 5, 55, 104, 151 });
  }

  @Test
  public void testCanopyPreClusteringOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> result = new ELKIBuilder<CanopyPreClustering<DoubleVector>>(CanopyPreClustering.class) //
        .with(CanopyPreClustering.Parameterizer.T1_ID, 25) //
        .with(CanopyPreClustering.Parameterizer.T2_ID, 25) //
        .build().run(db);
    testFMeasure(db, result, 0.97089);
    testClusterSizes(result, new int[] { 22, 200, 208, 208 });
  }
}
