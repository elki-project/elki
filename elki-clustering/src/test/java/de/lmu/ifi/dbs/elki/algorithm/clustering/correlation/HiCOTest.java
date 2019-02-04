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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.OPTICSXi;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for HiCO.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HiCOTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testHiCOResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-embedded-2-4d.ascii", 600);
    Clustering<OPTICSModel> result = new ELKIBuilder<OPTICSXi>(OPTICSXi.class) //
        .with(OPTICSXi.Parameterizer.XI_ID, 0.1) //
        .with(OPTICSXi.Parameterizer.XIALG_ID, HiCO.class) //
        .with(HiCO.Parameterizer.MU_ID, 20) //
        .with(HiCO.Parameterizer.K_ID, 20) //
        .with(HiCO.Parameterizer.DELTA_ID, 0.05) //
        .with(HiCO.Parameterizer.ALPHA_ID, 0.9) //
        .build().run(db);
    testFMeasure(db, result, 0.7151379);
    testClusterSizes(result, new int[] { 186, 414 });
  }

  @Test
  public void testHiCOOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
    Clustering<OPTICSModel> result = new ELKIBuilder<OPTICSXi>(OPTICSXi.class) //
        .with(OPTICSXi.Parameterizer.XI_ID, 0.1) //
        .with(OPTICSXi.Parameterizer.XIALG_ID, HiCO.class) //
        .with(HiCO.Parameterizer.MU_ID, 20) //
        .with(HiCO.Parameterizer.K_ID, 20) //
        .with(HiCO.Parameterizer.DELTA_ID, 0.25) //
        .with(HiCO.Parameterizer.ALPHA_ID, 0.9) //
        .build().run(db);
    testFMeasure(db, result, 0.736385);
    testClusterSizes(result, new int[] { 3, 168, 220, 259 });
  }
}
