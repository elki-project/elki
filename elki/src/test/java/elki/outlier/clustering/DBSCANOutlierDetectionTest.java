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
package elki.outlier.clustering;

import org.junit.Test;

import elki.clustering.dbscan.DBSCAN;
import elki.clustering.dbscan.GeneralizedDBSCAN;
import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the DBSCAN outlier detection algorithm.
 *
 * @author Braulio V.S. Vinces
 * @since 0.7.5
 */
public class DBSCANOutlierDetectionTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testDBSCANOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<DBSCANOutlierDetection>(DBSCANOutlierDetection.class) //
        .with(DBSCAN.Par.EPSILON_ID, 0.04) //
        .with(DBSCAN.Par.MINPTS_ID, 20) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.718);
    assertSingleScore(result, 416, 1.0); // noise point
    assertSingleScore(result, 16, 0.0); // non-noise point
  }

  @Test
  public void testDBSCANOutlierDetectionWithCoreObjects() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<DBSCANOutlierDetection>(DBSCANOutlierDetection.class) //
        .with(DBSCAN.Par.EPSILON_ID, 0.04) //
        .with(DBSCAN.Par.MINPTS_ID, 20) //
        .with(GeneralizedDBSCAN.Par.COREMODEL_ID) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.7772);
    assertSingleScore(result, 416, 1.0); // noise point
    assertSingleScore(result, 29, 0.5); // border point
    assertSingleScore(result, 16, 0.0); // core point
  }
}
