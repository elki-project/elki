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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.AbstractOutlierAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Tests the DBOutlierDetection algorithm.
 * 
 * @author Lucia Cichella
 * @since 0.7.0
 */
public class DBOutlierDetectionTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testDBOutlierDetection() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);
    OutlierResult result = new ELKIBuilder<DBOutlierDetection<DoubleVector>>(DBOutlierDetection.class) //
        .with(DBOutlierDetection.Parameterizer.D_ID, 0.175) //
        .with(DBOutlierDetection.Parameterizer.P_ID, 0.98) //
        .build().run(db);
    testSingleScore(result, 1025, 0.0);
    testAUC(db, "Noise", result, 0.97487179);
  }

  /**
   * Test with a kNN index.
   */
  @Test
  public void testDBOutlierKNN() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    MaterializeKNNPreprocessor<NumberVector> preproc = new MaterializeKNNPreprocessor<>(rel, EuclideanDistanceFunction.STATIC, 179);
    db.getHierarchy().add(rel, preproc);
    preproc.initialize();
    OutlierResult result = new ELKIBuilder<DBOutlierDetection<DoubleVector>>(DBOutlierDetection.class) //
        .with(DBOutlierDetection.Parameterizer.D_ID, 0.175) //
        .with(DBOutlierDetection.Parameterizer.P_ID, 0.98) //
        .build().run(db);
    testSingleScore(result, 1025, 0.0);
    testAUC(db, "Noise", result, 0.97487179);
  }

  /**
   * Test with a range index.
   */
  @Test
  public void testDBOutlierRange() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-fire.ascii", 1025);
    // This is a bit of a hack to make a range-only index.
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    PrecomputedDistanceMatrix<NumberVector> idx = new PrecomputedDistanceMatrix<NumberVector>(rel, (DBIDRange) rel.getDBIDs(), EuclideanDistanceFunction.STATIC) {
      @Override
      public KNNQuery<NumberVector> getKNNQuery(DistanceQuery<NumberVector> distanceQuery, Object... hints) {
        return null; // Disable kNN queries, to force range queries to be
                     // tested.
      }
    };
    db.getHierarchy().add(rel, idx);
    idx.initialize();
    OutlierResult result = new ELKIBuilder<DBOutlierDetection<DoubleVector>>(DBOutlierDetection.class) //
        .with(DBOutlierDetection.Parameterizer.D_ID, 0.175) //
        .with(DBOutlierDetection.Parameterizer.P_ID, 0.98) //
        .build().run(db);
    testSingleScore(result, 1025, 0.0);
    testAUC(db, "Noise", result, 0.97487179);
  }
}
