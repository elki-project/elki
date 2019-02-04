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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.BinarySplitSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for space-filling curve NN search
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SpacefillingMaterializeKNNPreprocessorTest {
  // the following values depend on the data set used!
  static String dataset = "elki/testdata/unittests/3clusters-and-noise-2d.csv";

  // number of kNN to query
  int k = 10;

  // the size of objects inserted and deleted
  int updatesize = 12;

  int seed = 5;

  // size of the data set
  int shoulds = 330;

  @Test
  public void testPreprocessor() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, shoulds);

    Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = db.getDistanceQuery(rel, EuclideanDistanceFunction.STATIC);

    // get linear queries
    LinearScanDistanceKNNQuery<DoubleVector> lin_knn_query = new LinearScanDistanceKNNQuery<>(distanceQuery);

    // get preprocessed queries
    SpacefillingMaterializeKNNPreprocessor<DoubleVector> preproc = //
        new ELKIBuilder<SpacefillingMaterializeKNNPreprocessor.Factory<DoubleVector>>(SpacefillingMaterializeKNNPreprocessor.Factory.class) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.CURVES_ID, Arrays.asList( //
                HilbertSpatialSorter.class, PeanoSpatialSorter.class, ZCurveSpatialSorter.class, BinarySplitSpatialSorter.class)) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.K_ID, k) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.VARIANTS_ID, 10) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.WINDOW_ID, 1.) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Parameterizer.RANDOM_ID, 0L) //
            .build().instantiate(rel);
    preproc.initialize();
    // add as index
    db.getHierarchy().add(rel, preproc);
    KNNQuery<DoubleVector> preproc_knn_query = preproc.getKNNQuery(distanceQuery, k);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanDistanceKNNQuery);

    // test queries
    SpacefillingKNNPreprocessorTest.testKNNQueries(rel, lin_knn_query, preproc_knn_query, k);
    // also test partial queries, forward only
    SpacefillingKNNPreprocessorTest.testKNNQueries(rel, lin_knn_query, preproc_knn_query, k / 2);
  }
}
