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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.OPTICSXi;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test HiSC on a simple test data set.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HiSCTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testHiSC() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-4-5d.ascii", 1100);
    Clustering<?> result = new ELKIBuilder<OPTICSXi>(OPTICSXi.class) //
        .with(OPTICSXi.Parameterizer.XIALG_ID, HiSC.class) //
        .with(OPTICSXi.Parameterizer.XI_ID, 0.1) //
        .with(HiSC.Parameterizer.ALPHA_ID, 0.05) //
        .with(HiSC.Parameterizer.K_ID, 5) //
        .build().run(db);
    testFMeasure(db, result, .7364473);
    testClusterSizes(result, new int[] { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 8, 14, 17, 78, 178, 201, 401 });
  }
}
