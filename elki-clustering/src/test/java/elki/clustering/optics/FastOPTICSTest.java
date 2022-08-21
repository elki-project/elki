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
package elki.clustering.optics;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.database.Database;
import elki.index.preprocessed.fastoptics.RandomProjectedNeighborsAndDensities;
import elki.utilities.ELKIBuilder;

/**
 * Simple regression test for FastOPTICS.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FastOPTICSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testFastOPTICS() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> clustering = new ELKIBuilder<>(OPTICSXi.class) //
        .with(OPTICSList.Par.MINPTS_ID, 20) //
        .with(OPTICSXi.Par.XI_ID, 0.1) //
        .with(OPTICSXi.Par.XIALG_ID, FastOPTICS.class) //
        .with(RandomProjectedNeighborsAndDensities.Par.RANDOM_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.856917);
    assertClusterSizes(clustering, new int[] { 4, 4, 5, 6, 7, 7, 10, 15, 23, 25, 26, 57, 73, 191, 257 });
  }
}
