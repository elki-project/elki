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
package elki.evaluation.clustering;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.trivial.ByLabelClustering;
import elki.clustering.trivial.TrivialAllInOne;
import elki.database.Database;
import elki.math.MeanVariance;

/**
 * Validate {@link ClusterContingencyTable} with respect to its ability to
 * compare data clusterings.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ClusterContingencyTableTest {
  // the following values depend on the data set used!
  String dataset = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  @Test
  public void testGiniMeasure() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds);
    ClusterContingencyTable table = new ClusterContingencyTable(true, false, //
        new TrivialAllInOne().autorun(db), new ByLabelClustering().autorun(db));

    MeanVariance v1 = table.averageSymmetricGini();
    assertEquals(2 / 3., v1.getMean(), 0);
    assertEquals(0.11111111111111112, v1.getPopulationVariance(), 0);

    MeanVariance v2 = table.adjustedSymmetricGini();
    assertEquals(Double.NaN, v2.getMean(), 0);
    assertEquals(Double.NaN, v2.getPopulationVariance(), 0);
  }
}
