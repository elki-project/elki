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
package elki.clustering.silhouette;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.FirstK;
import elki.clustering.kmedoids.FasterPAM;
import elki.clustering.kmedoids.initialization.KMedoidsKMedoidsInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MedoidModel;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.query.distance.PrimitiveDistanceQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.clustering.internal.Silhouette;
import elki.utilities.ELKIBuilder;

/**
 * Test PAMMEDSIL clustering.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class PAMMEDSILTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testPAMMEDSIL() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<PAMMEDSIL<NumberVector>>(PAMMEDSIL.class) //
        .with(KMedoidsKMedoidsInitialization.Par.INNER_ID, FasterPAM.class) //
        .with(KMeans.K_ID, 3) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.91385864);
    assertClusterSizes(result, new int[] { 57, 115, 158 });
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double sil = new Silhouette<NumberVector>(EuclideanDistance.STATIC, false) //
        .evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, EuclideanDistance.STATIC), result);
    assertEquals("Silhouette not as expected", 0.8484889738366984, sil, 1e-15);
  }

  @Test
  public void testPAMMEDSILworseStart() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<PAMMEDSIL<NumberVector>>(PAMMEDSIL.class) //
        .with(KMeans.INIT_ID, FirstK.class) //
        .with(KMeans.K_ID, 3) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.91385864);
    assertClusterSizes(result, new int[] { 57, 115, 158 });
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double sil = new Silhouette<NumberVector>(EuclideanDistance.STATIC, false) //
        .evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, EuclideanDistance.STATIC), result);
    assertEquals("Silhouette not as expected", 0.8484889738366984, sil, 1e-15);
  }

  @Test
  public void testPAMMEDSILtwo() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<PAMMEDSIL<NumberVector>>(PAMMEDSIL.class) //
        .with(KMeans.INIT_ID, FirstK.class) //
        .with(KMeans.K_ID, 2) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.785693747);
    assertClusterSizes(result, new int[] { 159, 171 });
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double sil = new Silhouette<NumberVector>(EuclideanDistance.STATIC, false) //
        .evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, EuclideanDistance.STATIC), result);
    assertEquals("Silhouette not as expected", 0.7664603331156499, sil, 1e-15);
  }

  @Test
  public void testPAMMEDSILfour() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<MedoidModel> result = new ELKIBuilder<PAMMEDSIL<NumberVector>>(PAMMEDSIL.class) //
        .with(KMeans.INIT_ID, FirstK.class) //
        .with(KMeans.K_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.923322767);
    assertClusterSizes(result, new int[] { 5, 56, 111, 158 });
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double sil = new Silhouette<NumberVector>(EuclideanDistance.STATIC, false) //
        .evaluateClustering(rel, new PrimitiveDistanceQuery<>(rel, EuclideanDistance.STATIC), result);
    assertEquals("Silhouette not as expected", 0.8473403086724154, sil, 1e-15);
  }
}
