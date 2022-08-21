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
package elki.index.preprocessed.knn;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDRef;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.LinearScanKNNByDBID;
import elki.database.query.knn.LinearScanKNNByObject;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.math.spacefillingcurves.BinarySplitSpatialSorter;
import elki.math.spacefillingcurves.HilbertSpatialSorter;
import elki.math.spacefillingcurves.PeanoSpatialSorter;
import elki.math.spacefillingcurves.ZCurveSpatialSorter;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;

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

    Relation<DoubleVector> relation = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    DistanceQuery<DoubleVector> distanceQuery = new QueryBuilder<>(relation, EuclideanDistance.STATIC).distanceQuery();

    // get linear queries
    KNNSearcher<DBIDRef> lin_knn_query = new LinearScanKNNByDBID<>(distanceQuery);

    // get preprocessed queries
    SpacefillingMaterializeKNNPreprocessor<DoubleVector> preproc = //
        new ELKIBuilder<SpacefillingMaterializeKNNPreprocessor.Factory<DoubleVector>>(SpacefillingMaterializeKNNPreprocessor.Factory.class) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Par.CURVES_ID, Arrays.asList( //
                HilbertSpatialSorter.class, PeanoSpatialSorter.class, ZCurveSpatialSorter.class, BinarySplitSpatialSorter.class)) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.K_ID, k) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Par.VARIANTS_ID, 10) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Par.WINDOW_ID, 1.) //
            .with(SpacefillingMaterializeKNNPreprocessor.Factory.Par.RANDOM_ID, 0L) //
            .build().instantiate(relation);
    preproc.initialize();
    // add as index
    Metadata.hierarchyOf(relation).addChild(preproc);
    KNNSearcher<DBIDRef> preproc_knn_query = preproc.kNNByDBID(distanceQuery, k, 0);
    assertFalse("Preprocessor knn query class incorrect.", preproc_knn_query instanceof LinearScanKNNByObject);

    // test queries
    SpacefillingKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k);
    // also test partial queries, forward only
    SpacefillingKNNPreprocessorTest.testKNNQueries(relation, lin_knn_query, preproc_knn_query, k / 2);
  }
}
