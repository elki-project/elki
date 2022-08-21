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
package elki.evaluation.clustering.internal;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.trivial.ByLabelClustering;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.filter.FixedDBIDsFilter;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.clustering.EvaluateClustering.ScoreResult;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.Measurement;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test for {@link DBCV} with ByLabelClustering
 * <p>
 * Note:<br>
 * This measurement is unpredictable, as it depends on the order of the data
 * set because there may be multiple different minimum spanning trees.
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class DBCVTest {
  final static String dataset = "elki/testdata/unittests/uebungsblatt-2d-mini-jitter.csv";

  /**
   * Test for {@link DBCV} with ByLabelClustering
   */
  @Test
  public void testEvaluateDBCV() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    // param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
    // new
    // ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID,
    // 0).build());
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, new FixedDBIDsFilter(1));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, 20, param);
    DBCV<NumberVector> dbcv = new ELKIBuilder<>(DBCV.class). //
        with(DBCV.Par.DISTANCE_ID, dist).build();
    // create clustering
    ByLabelClustering clustering = new ELKIBuilder<>(ByLabelClustering.class). //
        with(ByLabelClustering.Par.NOISE_ID, Pattern.compile("Outlier")).build();
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.GUESSED_LABEL));
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());
    // evaluate clustering
    dbcv.evaluateClustering(rel, rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup dbcvimg = er.findOrCreateGroup("Distance-based");
    // check measurements
    assertTrue(dbcvimg.hasMeasure("Density Based Clustering Validation"));
    Measurement m = dbcvimg.getMeasure("Density Based Clustering Validation");
    assertNotNull("No Density Based Clustering Validation value", m);
    assertEquals("Density Based Clustering Validation value not as expected", 0.699142946543063, m.getVal(), 1e-15);
  }
}
