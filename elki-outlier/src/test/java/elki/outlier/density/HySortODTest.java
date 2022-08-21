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
package elki.outlier.density;

import org.junit.Test;

import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the HySortOD algorithm.
 * 
 * @author Braulio V.S. Vinces
 * @since 0.8.0
 */
public class HySortODTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testHySortOD() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<HySortOD>(HySortOD.class) //
        .with(HySortOD.Par.B_ID, 5) //
        .with(HySortOD.Par.MIN_SPLIT_ID, 100) //
        .build().autorun(db);
    assertSingleScore(result, 945, 0.9545454545454546);
    assertAUC(db, "Noise", result, 0.922537037037037);
  }

  @Test
  public void testHySortODNaive() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<HySortOD>(HySortOD.class) //
        .with(HySortOD.Par.B_ID, 5) //
        .with(HySortOD.Par.MIN_SPLIT_ID, 0) //
        .build().autorun(db);
    assertSingleScore(result, 945, 0.9545454545454546);
    assertAUC(db, "Noise", result, 0.922537037037037);
  }
}
