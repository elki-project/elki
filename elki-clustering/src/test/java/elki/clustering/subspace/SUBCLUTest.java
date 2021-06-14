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
package elki.clustering.subspace;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.SubspaceModel;
import elki.database.Database;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.filter.selection.RandomSamplingStreamFilter;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SUBCLU run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SUBCLU performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SUBCLUTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSUBCLUResults() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<SubspaceModel> result = new ELKIBuilder<SUBCLU<NumberVector>>(SUBCLU.class) //
        .with(SUBCLU.Par.EPSILON_ID, 0.001) //
        .with(SUBCLU.Par.MINPTS_ID, 100) //
        .build().autorun(db);
    // PairCounting is not appropriate here: overlapping clusterings!
    assertClusterSizes(result, new int[] { 6, 191, 194, 395 });
  }

  @Test
  public void testSUBCLUSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<SubspaceModel> result = new ELKIBuilder<SUBCLU<NumberVector>>(SUBCLU.class) //
        .with(SUBCLU.Par.EPSILON_ID, 0.04) //
        .with(SUBCLU.Par.MINPTS_ID, 70) //
        .build().autorun(db);
    // PairCounting is not appropriate here: overlapping clusterings!
    assertClusterSizes(result, new int[] { 74, 99, 114, 136, 247, 303, 323 });
  }

  @Test
  public void testSUBCLUSubspaceAxisParallel() {
    ListParameterization inp = new ListParameterization();
    inp.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, RandomSamplingStreamFilter.class) //
    .addParameter(RandomSamplingStreamFilter.Par.PROB_ID, .25) //
    .addParameter(RandomSamplingStreamFilter.Par.SEED_ID, 0);
    Database db = makeSimpleDatabase(UNITTEST + "axis-parallel-subspace-clusters-6d.csv.gz", 630, inp);
    Clustering<SubspaceModel> result = new ELKIBuilder<SUBCLU<DoubleVector>>(SUBCLU.class) //
        .with(SUBCLU.Par.EPSILON_ID, 5) //
        .with(SUBCLU.Par.MINPTS_ID, 50) //
        .with(SUBCLU.Par.MINDIM_ID, 2) //
        .build().autorun(db);
    // PairCounting is not appropriate here: overlapping clusterings!
    assertClusterSizes(result, new int[] { 59, 79, 146, 152, 161, 167 });
  }
}
