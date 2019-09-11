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
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the Feature Bagging algorithm.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FeatureBaggingTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testFeatureBaggingSum() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(FeatureBagging.class) //
        .with(LOF.Par.K_ID, 10) //
        .with(FeatureBagging.Par.NUM_ID, 10) //
        .with(FeatureBagging.Par.SEED_ID, 1) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.94758434);
    testSingleScore(result, 1293, 12.816102);
  }

  @Test
  public void testFeatureBaggingBreadth() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);
    OutlierResult result = new ELKIBuilder<>(FeatureBagging.class) //
        .with(LOF.Par.K_ID, 10) //
        .with(FeatureBagging.Par.NUM_ID, 10) //
        .with(FeatureBagging.Par.SEED_ID, 5) //
        .with(FeatureBagging.Par.BREADTH_ID) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.92470588);
    testSingleScore(result, 1293, 1.2047264);
  }
}
