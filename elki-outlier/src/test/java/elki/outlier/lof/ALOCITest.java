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
 * Tests the ALOCI algorithm.
 *
 * @author Lucia Cichella
 * @since 0.7.5
 */
public class ALOCITest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testALOCI() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    OutlierResult result = new ELKIBuilder<ALOCI<DoubleVector>>(ALOCI.class) //
        .with(ALOCI.Par.SEED_ID, 1) //
        .with(ALOCI.Par.GRIDS_ID, 3) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.7777777);
    assertSingleScore(result, 146, 1.1242238);
  }
}
