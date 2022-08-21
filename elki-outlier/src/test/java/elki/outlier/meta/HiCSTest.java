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
package elki.outlier.meta;

import org.junit.Test;

import elki.database.Database;
import elki.math.statistics.tests.KolmogorovSmirnovTest;
import elki.math.statistics.tests.WelchTTest;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.outlier.lof.LOF;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the HiCS algorithm.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class HiCSTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testHiCSKS() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(HiCS.class) //
        .with(LOF.Par.K_ID, 10) //
        .with(HiCS.Par.LIMIT_ID, 10) //
        .with(HiCS.Par.SEED_ID, 3) //
        .with(HiCS.Par.TEST_ID, KolmogorovSmirnovTest.STATIC) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.85340056);
    assertSingleScore(result, 1293, 4.935802);
  }

  @Test
  public void testHiCSWelch() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(HiCS.class) //
        .with(LOF.Par.K_ID, 10) //
        .with(HiCS.Par.LIMIT_ID, 10) //
        .with(HiCS.Par.SEED_ID, 5) //
        .with(HiCS.Par.TEST_ID, WelchTTest.STATIC) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.88036);
    assertSingleScore(result, 1293, 3.7432);
  }
}
