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
package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Performs a full EM run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that EM's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.5
 */
public class EMTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testEMMLEMultivariate() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.K_ID, 6) //
        .build().run(db);
    testFMeasure(db, result, 0.967410486);
    testClusterSizes(result, new int[] { 3, 5, 91, 98, 200, 313 });
  }

  @Test
  public void testEMMAPMultivariate() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.PRIOR_ID, 10) //
        .with(EM.Parameterizer.K_ID, 5) //
        .build().run(db);
    testFMeasure(db, result, 0.958843);
    testClusterSizes(result, new int[] { 3, 95, 97, 202, 313 });
  }

  @Test
  public void testEMMLETwoPass() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.K_ID, 6) //
        .with(EM.Parameterizer.INIT_ID, TwoPassMultivariateGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.967410486);
    testClusterSizes(result, new int[] { 3, 5, 91, 98, 200, 313 });
  }

  @Test
  public void testEMMAPTwoPass() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.PRIOR_ID, 10) //
        .with(EM.Parameterizer.K_ID, 5) //
        .with(EM.Parameterizer.INIT_ID, TwoPassMultivariateGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.958843);
    testClusterSizes(result, new int[] { 3, 95, 97, 202, 313 });
  }

  @Test
  public void testEMMLETextbook() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.K_ID, 6) //
        .with(EM.Parameterizer.INIT_ID, TextbookMultivariateGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.967410486);
    testClusterSizes(result, new int[] { 3, 5, 91, 98, 200, 313 });
  }

  @Test
  public void testEMMAPTextbook() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Parameterizer.PRIOR_ID, 10) //
        .with(EM.Parameterizer.K_ID, 5) //
        .with(EM.Parameterizer.INIT_ID, TextbookMultivariateGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.958843);
    testClusterSizes(result, new int[] { 3, 95, 97, 202, 313 });
  }

  @Test
  public void testEMMLEDiagonal() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 3) //
        .with(EM.Parameterizer.K_ID, 5) //
        .with(EM.Parameterizer.INIT_ID, DiagonalGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.9681384);
    testClusterSizes(result, new int[] { 7, 91, 99, 200, 313 });
  }

  @Test
  public void testEMMAPDiagonal() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 3) //
        .with(EM.Parameterizer.K_ID, 5) //
        .with(EM.Parameterizer.INIT_ID, DiagonalGaussianModelFactory.class) //
        .with(EM.Parameterizer.PRIOR_ID, 10) //
        .build().run(db);
    testFMeasure(db, result, 0.949566);
    testClusterSizes(result, new int[] { 6, 97, 98, 202, 307 });
  }

  @Test
  public void testEMMLESpherical() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(EM.Parameterizer.K_ID, 4) //
        .with(EM.Parameterizer.INIT_ID, SphericalGaussianModelFactory.class) //
        .build().run(db);
    testFMeasure(db, result, 0.811247176);
    testClusterSizes(result, new int[] { 8, 95, 198, 409 });
  }

  @Test
  public void testEMMAPSpherical() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(EM.Parameterizer.K_ID, 4) //
        .with(EM.Parameterizer.INIT_ID, SphericalGaussianModelFactory.class) //
        .with(EM.Parameterizer.PRIOR_ID, 10) //
        .build().run(db);
    testFMeasure(db, result, 0.9357286);
    testClusterSizes(result, new int[] { 103, 104, 208, 295 });
  }
}
