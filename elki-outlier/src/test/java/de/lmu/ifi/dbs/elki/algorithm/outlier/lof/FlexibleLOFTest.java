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
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the LOF algorithm.
 *
 * @author Lucia Cichella
 * @since 0.7.5
 */
public class FlexibleLOFTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testFlexibleLOF() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<FlexibleLOF<DoubleVector>>(FlexibleLOF.class) //
        .with(FlexibleLOF.Parameterizer.KREF_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.8921680672268908);
    testSingleScore(result, 1293, 1.1945314199156365);

    result = new ELKIBuilder<FlexibleLOF<DoubleVector>>(FlexibleLOF.class) //
        .with(FlexibleLOF.Parameterizer.KREACH_ID, 15)//
        .with(FlexibleLOF.Parameterizer.KREF_ID, 10)//
        .with(FlexibleLOF.Parameterizer.REACHABILITY_DISTANCE_FUNCTION_ID, SquaredEuclideanDistanceFunction.class)//
        .build().run(db);
    testAUC(db, "Noise", result, 0.8904);
    testSingleScore(result, 1293, 1.427456);
  }
}
