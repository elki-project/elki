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
package elki.evaluation.clustering.internal;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.clustering.trivial.ByLabelClustering;
import elki.clustering.trivial.TrivialAllInOne;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.Model;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.query.distance.PrimitiveDistanceQuery;
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
 * Test for {@link Silhouette} with ByLabelClustering and KMeans
 * clustering
 * 
 * @author Robert Gehde
 */
public class SilhouetteTest {
  final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  /**
   * Test for {@link Silhouette} with ByLabelClustering and
   * TREAT_NOISE_AS_SINGLETONS option
   */
  @Test
  public void testEvaluateSilhouetteTestSingleton() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20, param);
    Silhouette<NumberVector> silh = new ELKIBuilder<>(Silhouette.class). //
        with(Silhouette.Par.DISTANCE_ID, dist). //
        with(Silhouette.Par.NOISE_ID, NoiseHandling.TREAT_NOISE_AS_SINGLETONS).build();
    // create clustering
    ByLabelClustering clustering = new ELKIBuilder<>(ByLabelClustering.class). //
        with(ByLabelClustering.Par.NOISE_ID, Pattern.compile("Outlier")).build();
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.CLASSLABEL));
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());
    // evaluate clustering
    silh.evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, dist), rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup silhouette = er.findOrCreateGroup("Distance-based");
    // check measurements
    Iterator<Measurement> silit = silhouette.iterator();
    assertTrue("No silhouette measurement", silit.hasNext());
    Measurement m = silit.next();
    assertFalse("Too many measurements", silit.hasNext());

    assertEquals("Silhouette not as expected", 0.520636492550455, m.getVal(), 1e-15);
  }

  /**
   * Test for {@link Silhouette} with ByLabelClustering and MERGE_NOISE
   * option
   */
  @Test
  public void testEvaluateSilhouetteTest() {
    // load classes and data
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20, param);
    Silhouette<NumberVector> silh = new ELKIBuilder<>(Silhouette.class). //
        with(Silhouette.Par.DISTANCE_ID, dist). //
        with(Silhouette.Par.NOISE_ID, NoiseHandling.MERGE_NOISE).build();
    // create clustering
    ByLabelClustering clustering = new ELKIBuilder<>(ByLabelClustering.class). //
        with(ByLabelClustering.Par.NOISE_ID, Pattern.compile("Outlier")).build();
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.CLASSLABEL));
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());
    // evaluate clustering
    silh.evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, dist), rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup silhouette = er.findOrCreateGroup("Distance-based");
    // check measurements
    Iterator<Measurement> silit = silhouette.iterator();
    assertTrue("No silhouette measurement", silit.hasNext());
    Measurement m = silit.next();
    assertFalse("Too many measurements", silit.hasNext());

    assertEquals("Silhouette not as expected", 0.589897756171037, m.getVal(), 1e-13);
  }

  /**
   * Regression test for {@link Silhouette} with KMeans clustering and
   * TREAT_NOISE_AS_SINGLETONS option
   */
  @Test
  public void testEvaluateSilhouetteTestKMeans() {
    EuclideanDistance dist = EuclideanDistance.STATIC;
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Silhouette<NumberVector> silh = new ELKIBuilder<>(Silhouette.class).with(Silhouette.Par.DISTANCE_ID, dist).with(Silhouette.Par.NOISE_ID, NoiseHandling.TREAT_NOISE_AS_SINGLETONS).build();

    LloydKMeans<NumberVector> clustering = new LloydKMeans<>(dist, 3, 20, new RandomlyChosen<>(new RandomFactory(12341234L)));
    Clustering<?> rbl = clustering.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());
    // evaluate clustering
    silh.evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, dist), rbl);

    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup silhouette = er.findOrCreateGroup("Distance-based");
    // check measurements
    Iterator<Measurement> silit = silhouette.iterator();
    assertTrue("No silhouette measurement", silit.hasNext());
    Measurement m = silit.next();
    assertFalse("Too many measurements", silit.hasNext());

    assertEquals("Silhouette not as expected", 0.6970597031375269, m.getVal(), 1e-15);
  }

  /**
   * Test for {@link Silhouette} with a single cluster only.
   */
  @Test
  public void testEvaluateSilhouetteSingleCluster() {
    EuclideanDistance dist = EuclideanDistance.STATIC;
    ListParameterization param = new ListParameterization();
    param.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, //
        new ELKIBuilder<ClassLabelFilter>(ClassLabelFilter.class).with(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0).build());
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20, param);
    Relation<NumberVector> rel = db.getRelation(dist.getInputTypeRestriction());
    Silhouette<NumberVector> silh = new ELKIBuilder<>(Silhouette.class).with(Silhouette.Par.DISTANCE_ID, dist).build();
    Clustering<Model> rbl = new TrivialAllInOne().run(rel); // Fake clustering
    silh.evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, dist), rbl);
    // get measurement data
    It<ScoreResult> it = Metadata.hierarchyOf(rbl).iterChildren().filter(EvaluationResult.class);
    assertTrue("No evaluation result", it.valid());
    assertTrue("Not a score result", it.get() instanceof EvaluationResult);
    EvaluationResult er = it.get();
    it.advance();
    assertFalse("More than one evaluation result?", it.valid());

    MeasurementGroup silhouette = er.findOrCreateGroup("Distance-based");
    // check measurements
    Iterator<Measurement> silit = silhouette.iterator();
    assertTrue("No silhouette measurement", silit.hasNext());
    Measurement m = silit.next();
    assertFalse("Too many measurements", silit.hasNext());
    assertEquals("Silhouette not as expected", 0., m.getVal(), 0);
  }
}
