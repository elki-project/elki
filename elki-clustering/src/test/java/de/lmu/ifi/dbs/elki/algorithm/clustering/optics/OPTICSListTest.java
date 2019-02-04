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
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full OPTICS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that OPTICS's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class OPTICSListTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testOPTICS() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> clustering = new ELKIBuilder<>(OPTICSXi.class) //
        .with(OPTICSList.Parameterizer.MINPTS_ID, 18) //
        .with(OPTICSList.Parameterizer.EPSILON_ID, 0.1) //
        .with(OPTICSXi.Parameterizer.XI_ID, 0.038) //
        .with(OPTICSXi.Parameterizer.XIALG_ID, OPTICSList.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.8891673);
    testClusterSizes(clustering, new int[] { 108, 117, 209, 276 });
  }
}
