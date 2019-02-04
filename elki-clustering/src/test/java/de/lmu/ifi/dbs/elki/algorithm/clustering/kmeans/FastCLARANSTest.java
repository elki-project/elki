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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for CLARANS+
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FastCLARANSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testCLARANS() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<MedoidModel> result = new ELKIBuilder<FastCLARANS<DoubleVector>>(FastCLARANS.class) //
        .with(KMeans.K_ID, 5) //
        .with(CLARANS.Parameterizer.RANDOM_ID, 0) //
        .with(CLARANS.Parameterizer.NEIGHBORS_ID, 3) //
        .with(CLARANS.Parameterizer.RESTARTS_ID, 5) //
        .build().run(db);
    // FastCLARANS finds better solution than CLARANS in this unit test!
    testFMeasure(db, result, .998);
    testClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testCLARANSNoise() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<FastCLARANS<DoubleVector>>(FastCLARANS.class) //
        .with(KMeans.K_ID, 3) //
        .with(CLARANS.Parameterizer.RANDOM_ID, 0) //
        .with(CLARANS.Parameterizer.NEIGHBORS_ID, .1) //
        .with(CLARANS.Parameterizer.RESTARTS_ID, 5) //
        .build().run(db);
    testFMeasure(db, result, 0.913858);
    testClusterSizes(result, new int[] { 57, 115, 158 });
  }
}
