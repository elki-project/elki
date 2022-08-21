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
package elki.outlier.subspace;

import org.junit.Test;

import elki.database.Database;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

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
    OutlierResult result = new ELKIBuilder<>(AggarwalYuEvolutionary.class) //
        .with(AggarwalYuEvolutionary.Par.K_ID, 2) //
        .with(AggarwalYuEvolutionary.Par.PHI_ID, 8) //
        .with(AggarwalYuEvolutionary.Par.M_ID, 20) //
        .with(AggarwalYuEvolutionary.Par.SEED_ID, 1) //
        .build().autorun(db);
    assertAUC(db, "Noise", result, 0.653888888888);
    assertSingleScore(result, 945, 0.0);
  }
}
