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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for LSDBC.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LSDBCTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testLSDBCResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<LSDBC<DoubleVector>>(LSDBC.class) //
        .with(LSDBC.Parameterizer.ALPHA_ID, 0.4) //
        .with(LSDBC.Parameterizer.K_ID, 20) //
        .build().run(db);
    testFMeasure(db, result, 0.44848979);
    testClusterSizes(result, new int[] { 38, 38, 41, 54, 159 });
  }

  @Test
  public void testLSDBCOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<Model> result = new ELKIBuilder<LSDBC<DoubleVector>>(LSDBC.class) //
        .with(LSDBC.Parameterizer.ALPHA_ID, 0.2) //
        .with(LSDBC.Parameterizer.K_ID, 120) //
        .build().run(db);
    testFMeasure(db, result, 0.95681073);
    testClusterSizes(result, new int[] { 32, 197, 203, 206 });
  }
}
