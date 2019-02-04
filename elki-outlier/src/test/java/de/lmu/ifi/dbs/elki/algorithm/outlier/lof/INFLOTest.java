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
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the INFLO algorithm.
 * 
 * @author Lucia Cichella
 * @since 0.7.0
 */
public class INFLOTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testINFLO() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<INFLO<DoubleVector>>(INFLO.class) //
        .with(INFLO.Parameterizer.K_ID, 30).build().run(db);
    testAUC(db, "Noise", result, 0.9606111);
    testSingleScore(result, 945, 1.3285178);
  }

  @Test
  public void testINFLOPruning() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<INFLO<DoubleVector>>(INFLO.class) //
        .with(INFLO.Parameterizer.M_ID, 0.5) //
        .with(INFLO.Parameterizer.K_ID, 30).build().run(db);
    testAUC(db, "Noise", result, 0.94130555);
    testSingleScore(result, 945, 1.3285178); // Not pruned.

    result = new ELKIBuilder<INFLO<DoubleVector>>(INFLO.class) //
        .with(INFLO.Parameterizer.M_ID, 0.2) //
        .with(INFLO.Parameterizer.K_ID, 30).build().run(db);
    testAUC(db, "Noise", result, 0.8198611111);
    testSingleScore(result, 945, 1.0); // Pruned.
  }
}
