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
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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
        .with(KDEOS.Parameterizer.KERNEL_ID, EpanechnikovKernelDensityFunction.class) //
        .with(KDEOS.Parameterizer.KMIN_ID, 5) //
        .with(KDEOS.Parameterizer.KMAX_ID, 20) //
        .with(KDEOS.Parameterizer.KERNEL_SCALE_ID, 1.) //
        .with(KDEOS.Parameterizer.IDIM_ID, -1) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.804918767);
    testSingleScore(result, 1293, 0.88750800246);
  }
}
