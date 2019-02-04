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
package de.lmu.ifi.dbs.elki.algorithm.outlier.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the AggarwalYuEvolutionary algorithm.
 * 
 * @author Lucia Cichella
 * @since 0.7.0
 */
public class AggarwalYuEvolutionaryTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testAggarwalYuEvolutionary() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<AggarwalYuEvolutionary<DoubleVector>>(AggarwalYuEvolutionary.class) //
        .with(AggarwalYuEvolutionary.Parameterizer.K_ID, 2) //
        .with(AggarwalYuEvolutionary.Parameterizer.PHI_ID, 8) //
        .with(AggarwalYuEvolutionary.Parameterizer.M_ID, 20) //
        .with(AggarwalYuEvolutionary.Parameterizer.SEED_ID, 0) //
        .build().run(db);
    testAUC(db, "Noise", result, 0.653888888888);
    testSingleScore(result, 945, 0.0);
  }
}
