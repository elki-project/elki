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
package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.preprocessed.fastoptics.RandomProjectedNeighborsAndDensities;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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
        .with(OPTICSList.Parameterizer.MINPTS_ID, 18) //
        .with(OPTICSXi.Parameterizer.XI_ID, 0.038) //
        .with(OPTICSXi.Parameterizer.XIALG_ID, FastOPTICS.class) //
        .with(RandomProjectedNeighborsAndDensities.Parameterizer.RANDOM_ID, 0) //
        .build().run(db);
    testFMeasure(db, clustering, 0.75722304);
    testClusterSizes(clustering, new int[] { 4, 5, 5, 6, 7, 8, 13, 23, 26, 35, 42, 57, 94, 166, 219 });
  }
}
