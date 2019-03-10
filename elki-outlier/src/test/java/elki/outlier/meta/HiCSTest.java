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
package elki.outlier.meta;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.outlier.lof.LOF;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.math.statistics.tests.KolmogorovSmirnovTest;
import elki.math.statistics.tests.WelchTTest;
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
    OutlierResult result = new ELKIBuilder<HiCS<DoubleVector>>(HiCS.class) //
        .with(LOF.Parameterizer.K_ID, 10) //
        .with(HiCS.Parameterizer.LIMIT_ID, 10) //
        .with(HiCS.Parameterizer.SEED_ID, 0) //
        .with(HiCS.Parameterizer.TEST_ID, KolmogorovSmirnovTest.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.85340056);
    testSingleScore(result, 1293, 4.935802);
  }

  @Test
  public void testHiCSWelch() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<HiCS<DoubleVector>>(HiCS.class) //
        .with(LOF.Parameterizer.K_ID, 10) //
        .with(HiCS.Parameterizer.LIMIT_ID, 10) //
        .with(HiCS.Parameterizer.SEED_ID, 0) //
        .with(HiCS.Parameterizer.TEST_ID, WelchTTest.STATIC) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.867159);
    testSingleScore(result, 1293, 4.7877822);
  }
}
