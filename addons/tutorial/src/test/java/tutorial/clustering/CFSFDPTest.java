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
package tutorial.clustering;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for the CFSFDP algorithm.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class CFSFDPTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testCFSFDPResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> result = new ELKIBuilder<CFSFDP<DoubleVector>>(CFSFDP.class) //
        .with(CFSFDP.Par.DC_ID, 0.5) //
        .with(CFSFDP.Par.K_ID, 3) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.90353);
    assertClusterSizes(result, new int[] { 51, 117, 162 });
  }

  @Test
  public void testCFSFDPOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> result = new ELKIBuilder<CFSFDP<DoubleVector>>(CFSFDP.class) //
        .with(CFSFDP.Par.DC_ID, 25) //
        .with(CFSFDP.Par.K_ID, 3) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.93866);
    assertClusterSizes(result, new int[] { 200, 211, 227 });
  }
}
