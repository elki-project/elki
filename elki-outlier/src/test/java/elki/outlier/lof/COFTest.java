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
package elki.outlier.lof;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the COF algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class COFTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testCOF() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<COF<DoubleVector>>(COF.class) //
        .with(COF.Parameterizer.K_ID, 10).build().run(db);
    testSingleScore(result, 1293, 1.415457);
    testAUC(db, "Noise", result, 0.8696806);
  }
}
