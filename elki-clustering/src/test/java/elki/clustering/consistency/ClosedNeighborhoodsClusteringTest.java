/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.consistency;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.clustering.dbscan.predicates.MutualNearestNeighborPredicate;
import elki.clustering.dbscan.predicates.NearestNeighborPredicate;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.Model;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;

/**
 * Test nearest neighbor neighborhood sets.
 * 
 * @author Niklas Strahmann
 */
public class ClosedNeighborhoodsClusteringTest {
  final static String DATASET = "elki/testdata/unittests/uebungsblatt-2d-mini.csv";

  @Test
  public void testAmountOfClosedNeighborhoodSets2NN() {
    int kNeighbors = 2;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoods<DoubleVector> ncsGenerator = new ClosedNeighborhoods<>(new NearestNeighborPredicate<>(kNeighbors, distance));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Clustering<Model> result = ncsGenerator.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 2, result.getAllClusters().size());
  }

  @Test
  public void testAmountOfClosedNeighborhoodSets1NN() {
    int kNeighbors = 1;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoods<DoubleVector> ncsGenerator = new ClosedNeighborhoods<>(new NearestNeighborPredicate<>(kNeighbors, distance));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Clustering<Model> result = ncsGenerator.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 3, result.getAllClusters().size());
  }

  @Test
  public void testAmountOfClosedNeighborhoodSets2MN() {
    int kNeighbors = 2;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoods<DoubleVector> ncsGenerator = new ClosedNeighborhoods<>(new MutualNearestNeighborPredicate<>(kNeighbors, distance));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Clustering<Model> result = ncsGenerator.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 6, result.getAllClusters().size());
  }

  @Test
  public void testAmountOfClosedNeighborhoodSets3MN() {
    int kNeighbors = 3;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoods<DoubleVector> ncsGenerator = new ClosedNeighborhoods<>(new MutualNearestNeighborPredicate<>(kNeighbors, distance));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Clustering<Model> result = ncsGenerator.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 4, result.getAllClusters().size());
  }

  @Test
  public void testAmountOfClosedNeighborhoodSets4MN() {
    int kNeighbors = 4;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoods<DoubleVector> ncsGenerator = new ClosedNeighborhoods<>(new MutualNearestNeighborPredicate<>(kNeighbors, distance));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Clustering<Model> result = ncsGenerator.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 3, result.getAllClusters().size());
  }

  @Test
  public void testAmountOfClosedNeighborhoodSets5MN() {
    int kNeighbors = 5;
    NumberVectorDistance<NumberVector> distance = EuclideanDistance.STATIC;
    ClosedNeighborhoods<DoubleVector> ncsGenerator = new ClosedNeighborhoods<>(new MutualNearestNeighborPredicate<>(kNeighbors, distance));
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(DATASET, 20);
    Clustering<Model> result = ncsGenerator.run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D));
    assertEquals("An unexpected amount of neighborhood sets were found.", 2, result.getAllClusters().size());
  }
}
