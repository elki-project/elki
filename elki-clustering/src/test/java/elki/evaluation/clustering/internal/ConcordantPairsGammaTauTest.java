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
import elki.result.EvaluationResult.Measurement;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.random.RandomFactory;

/**
 * Test for {@link ConcordantPairsGammaTau} with ByLabelClustering and KMeans
 * Clustering
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class ConcordantPairsGammaTauTest {
  final static String dataset = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  /**
   * Regression test for {@link ConcordantPairsGammaTau} with ByLabelClustering
   */
  @Test
  public void testEvaluateConcordantPairs() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, 20, param);
    ConcordantPairsGammaTau ecp = new ELKIBuilder<>(ConcordantPairsGammaTau.class).with(ConcordantPairsGammaTau.Par.DISTANCE_ID, dist).with(ConcordantPairsGammaTau.Par.NOISE_ID, NoiseHandling.MERGE_NOISE).build();

    // create clustering
    ByLabelClustering clustering = new ELKIBuilder<>(ByLabelClustering.class). //
        with(ByLabelClustering.Par.NOISE_ID, Pattern.compile("Outlier")).build();
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.CLASSLABEL));
    Relation<? extends NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());

    // evaluate clustering
    ecp.evaluateClustering(rel, rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult r = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup concordance = r.findOrCreateGroup("Concordance");
    // check measurements
    assertTrue(concordance.hasMeasure("Gamma"));
    Measurement g = concordance.getMeasure("Gamma");
    assertNotNull("No Concordance Gamma Value", g);
    assertEquals("Gamma Value not as expected", 0.9683676514473292, g.getVal(), 1e-15);

    Measurement t = concordance.getMeasure("Tau");
    assertNotNull("No Concordance Tau Value", t);
    assertEquals("Tau Value not as expected", 0.5907925965590582, t.getVal(), 1e-15);
  }

  /**
   * Regression test for {@link ConcordantPairsGammaTau} with KMeans clustering
   */
  @Test
  public void testEvaluateConcordantPairsKMeans() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(dataset, 20, param);
    ConcordantPairsGammaTau ecp = new ELKIBuilder<>(ConcordantPairsGammaTau.class).with(ConcordantPairsGammaTau.Par.DISTANCE_ID, dist).with(ConcordantPairsGammaTau.Par.NOISE_ID, NoiseHandling.MERGE_NOISE).build();

    // create clustering
    LloydKMeans<NumberVector> clustering = new LloydKMeans<NumberVector>(dist, 3, 20, new RandomlyChosen<>(new RandomFactory(12341234L)));
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());

    // evaluate clustering
    ecp.evaluateClustering(rel, rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult r = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup concordance = r.findOrCreateGroup("Concordance");
    // check measurements
    assertTrue(concordance.hasMeasure("Gamma"));
    Measurement g = concordance.getMeasure("Gamma");
    assertNotNull("No Concordance Gamma Value", g);
    assertEquals("Gamma Value not as expected", 0.9918554339526597, g.getVal(), 1e-15);

    Measurement t = concordance.getMeasure("Tau");
    assertNotNull("No Concordance Tau Value", t);
    assertEquals("Tau Value not as expected", 0.655704137488994, t.getVal(), 1e-15);
  }
}
