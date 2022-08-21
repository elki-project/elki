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
import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.clustering.trivial.ByLabelClustering;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.filter.typeconversions.ClassLabelFilter;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.clustering.EvaluateClustering.ScoreResult;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.EvaluationResult.Measurement;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.random.RandomFactory;

/**
 * Test for {@link DaviesBouldinIndex} with ByLabelClustering and KMeans
 * clustering
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class DaviesBouldinIndexTest {
  final static String dataset = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  /**
   * Test for {@link DaviesBouldinIndex} with ByLabelClustering
   */
  @Test
  public void testEvaluateDavisBouldin() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, 20, param);
    DaviesBouldinIndex dabo = new ELKIBuilder<>(DaviesBouldinIndex.class). //
        with(DaviesBouldinIndex.Par.DISTANCE_ID, dist). //
        with(DaviesBouldinIndex.Par.NOISE_ID, NoiseHandling.MERGE_NOISE).build();
    // create clustering
    ByLabelClustering clustering = new ELKIBuilder<>(ByLabelClustering.class). //
        with(ByLabelClustering.Par.NOISE_ID, Pattern.compile("Outlier")).build();
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.CLASSLABEL));
    Relation<? extends NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());

    // evaluate clustering
    dabo.evaluateClustering(rel, rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup dabomg = er.findOrCreateGroup("Distance-based");
    // check measurements
    assertTrue(dabomg.hasMeasure("Davies Bouldin Index"));
    Measurement m = dabomg.getMeasure("Davies Bouldin Index");
    assertNotNull("No Davies Bouldin Index", m);
    assertEquals("Davies Bouldin Index (mean) not as expected", 1.02661179674861, m.getVal(), 1e-13);
  }

  /**
   * Test for {@link DaviesBouldinIndex} with KMeans clustering
   */
  @Test
  public void testEvaluateDavisBouldinKMeans() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, 20, param);
    DaviesBouldinIndex dabo = new ELKIBuilder<>(DaviesBouldinIndex.class). //
        with(DaviesBouldinIndex.Par.DISTANCE_ID, dist). //
        with(DaviesBouldinIndex.Par.NOISE_ID, NoiseHandling.MERGE_NOISE).build();
    // create clustering
    LloydKMeans<NumberVector> clustering = new LloydKMeans<NumberVector>(dist, 3, 20, new RandomlyChosen<>(new RandomFactory(12341234L)));
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());

    // evaluate clustering
    dabo.evaluateClustering(rel, rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup dabomg = er.findOrCreateGroup("Distance-based");
    // check measurements
    assertTrue(dabomg.hasMeasure("Davies Bouldin Index"));
    Measurement m = dabomg.getMeasure("Davies Bouldin Index");
    assertNotNull("No Davies Bouldin Index", m);
    assertEquals("Davies Bouldin Index (mean) not as expected", .4184031237431774, m.getVal(), 1e-13);
  }
}
