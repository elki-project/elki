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
package elki.outlier;

import org.junit.Test;

import elki.clustering.optics.AbstractOPTICS;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the OPTICS-OF algorithm.
 * 
 * @author Lucia Cichella
 * @since 0.7.0
 */
public class OPTICSOFTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testOPTICSOF() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-parabolic.ascii", 530);
    OutlierResult result = new ELKIBuilder<OPTICSOF<DoubleVector>>(OPTICSOF.class) //
        .with(AbstractOPTICS.Par.MINPTS_ID, 22).build().run(db);
    testSingleScore(result, 416, 1.6108343626651815);
    testAUC(db, "Noise", result, 0.9058);
  }
}
