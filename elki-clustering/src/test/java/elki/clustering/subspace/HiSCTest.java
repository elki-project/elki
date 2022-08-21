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
import elki.clustering.optics.OPTICSXi;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

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
    Clustering<?> result = new ELKIBuilder<>(OPTICSXi.class) //
        .with(OPTICSXi.Par.XIALG_ID, HiSC.class) //
        .with(OPTICSXi.Par.XI_ID, 0.5) //
        .with(HiSC.Par.ALPHA_ID, 0.05) //
        .with(HiSC.Par.K_ID, 15) //
        .build().autorun(db);
    assertFMeasure(db, result, .78449);
    assertClusterSizes(result, new int[] { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 5, 8, 11, 17, 27, 185, 233, 536 });
  }
}
