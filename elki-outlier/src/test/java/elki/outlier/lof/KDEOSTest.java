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
package elki.outlier.lof;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the KDEOS algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KDEOSTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testKDEOS() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<KDEOS<DoubleVector>>(KDEOS.class) //
        .with(KDEOS.Par.KERNEL_ID, EpanechnikovKernelDensityFunction.class) //
        .with(KDEOS.Par.KMIN_ID, 5) //
        .with(KDEOS.Par.KMAX_ID, 20) //
        .with(KDEOS.Par.KERNEL_SCALE_ID, 1.) //
        .with(KDEOS.Par.IDIM_ID, -1) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.804918767);
    assertSingleScore(result, 1293, 0.88750800246);
  }
}
