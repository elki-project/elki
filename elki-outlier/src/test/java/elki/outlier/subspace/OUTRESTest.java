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
package elki.outlier.subspace;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the OUTRES algorithm.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class OUTRESTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testOUTRES() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-hierarchy.csv", 450);
    OutlierResult result = new ELKIBuilder<OUTRES>(OUTRES.class) //
        .with(OUTRES.Par.D_ID, 1.) //
        .build().run(db);
    testSingleScore(result, 406, 0.3659126362146687);
    testAUC(db, "Noise$", result, 0.7919);
  }
}
