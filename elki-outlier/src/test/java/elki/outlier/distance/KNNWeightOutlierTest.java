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
package elki.outlier.distance;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the KNNWeightOutlier algorithm.
 * 
 * @author Lucia Cichella
 * @since 0.7.0
 */
public class KNNWeightOutlierTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testKNNWeightOutlier() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<KNNWeightOutlier<DoubleVector>>(KNNWeightOutlier.class) //
        .with(KNNWeightOutlier.Par.K_ID, 4).build().run(db);
    testSingleScore(result, 945, 2.384117261027324);
    testAUC(db, "Noise", result, 0.9912777777777778);
  }
}
