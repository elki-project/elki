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
package elki.outlier.svm;

import org.junit.Test;

import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.similarity.kernel.RadialBasisFunctionKernel;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for one-class SVM (OCSVM) outlier detection.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class OCSVMTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testGamma() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(OCSVM.class) //
        .with(OCSVM.Par.KERNEL_ID, RadialBasisFunctionKernel.class) //
        .with(OCSVM.Par.NU_ID, .05) //
        .with(RadialBasisFunctionKernel.Par.GAMMA_ID, 1 / 6.) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.9567);
    assertSingleScore(result, 1293, 0.12765);
  }

  @Test
  public void testSigma() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(OCSVM.class) //
        .with(OCSVM.Par.KERNEL_ID, RadialBasisFunctionKernel.class) //
        .with(OCSVM.Par.NU_ID, .05) //
        .with(RadialBasisFunctionKernel.Par.SIGMA_ID, Math.sqrt(3)) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.9567);
    assertSingleScore(result, 1293, 0.12765);
  }
}
