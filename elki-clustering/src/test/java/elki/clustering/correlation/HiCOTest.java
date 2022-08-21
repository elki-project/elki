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
import elki.clustering.optics.OPTICSXi;
import elki.data.Clustering;
import elki.data.model.OPTICSModel;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

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
    Clustering<OPTICSModel> result = new ELKIBuilder<>(OPTICSXi.class) //
        .with(OPTICSXi.Par.XI_ID, 0.1) //
        .with(OPTICSXi.Par.XIALG_ID, HiCO.class) //
        .with(HiCO.Par.MU_ID, 20) //
        .with(HiCO.Par.K_ID, 20) //
        .with(HiCO.Par.DELTA_ID, 0.05) //
        .with(HiCO.Par.ALPHA_ID, 0.9) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.73834);
    assertClusterSizes(result, new int[] { 0, 4, 4, 6, 30, 176, 380 });
  }

  @Test
  public void testHiCOOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
    Clustering<OPTICSModel> result = new ELKIBuilder<>(OPTICSXi.class) //
        .with(OPTICSXi.Par.XI_ID, 0.2) //
        .with(OPTICSXi.Par.XIALG_ID, HiCO.class) //
        .with(HiCO.Par.MU_ID, 20) //
        .with(HiCO.Par.K_ID, 20) //
        .with(HiCO.Par.DELTA_ID, 0.25) //
        .with(HiCO.Par.ALPHA_ID, 0.95) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.78091);
    assertClusterSizes(result, new int[] { 6, 6, 20, 25, 51, 140, 201, 201 });
  }
}
