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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the KNNWeightOutlier algorithm.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HilOutTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testHilOut() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<HilOut<DoubleVector>>(HilOut.class) //
        .with(HilOut.Parameterizer.K_ID, 4) //
        .with(HilOut.Parameterizer.N_ID, 200) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.985398148);
    testSingleScore(result, 945, 1.70927657);
  }

  @Test
  public void testHilOutH16() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<HilOut<DoubleVector>>(HilOut.class) //
        .with(HilOut.Parameterizer.K_ID, 4) //
        .with(HilOut.Parameterizer.N_ID, 200) //
        .with(HilOut.Parameterizer.H_ID, 16) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.985398148);
    testSingleScore(result, 945, 1.70927657);
  }

  @Test
  public void testHilOutH8() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<HilOut<DoubleVector>>(HilOut.class) //
        .with(HilOut.Parameterizer.K_ID, 4) //
        .with(HilOut.Parameterizer.N_ID, 200) //
        .with(HilOut.Parameterizer.H_ID, 8) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.985398148);
    testSingleScore(result, 945, 1.70927657);
  }

  // Coarse enough to cause performance degradation:
  @Test
  public void testHilOutH2() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<HilOut<DoubleVector>>(HilOut.class) //
        .with(HilOut.Parameterizer.K_ID, 4) //
        .with(HilOut.Parameterizer.N_ID, 200) //
        .with(HilOut.Parameterizer.H_ID, 2) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.8246);
    testSingleScore(result, 945, 0.);
  }
}
