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
package elki.clustering.em.models;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.em.EM;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test the full multivariate Gaussian model.
 *
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MultivariateGaussianModelTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testHierarchicalMLE() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 4) //
        .with(EM.Par.K_ID, 6) //
        .with(EM.Par.MODEL_ID, MultivariateGaussianModelFactory.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.941968);
    assertClusterSizes(result, new int[] { 5, 13, 93, 98, 200, 301 });
  }

  @Test
  public void testHierarchicalMAP() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 4) //
        .with(EM.Par.K_ID, 5) //
        .with(EM.Par.MODEL_ID, MultivariateGaussianModelFactory.class) //
        .with(EM.Par.PRIOR_ID, 10) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.958843);
    assertClusterSizes(result, new int[] { 3, 95, 97, 202, 313 });
  }

  @Test
  public void testConstantMLE() {
    Database db = makeSimpleDatabase(UNITTEST + "constant-attribute.csv.gz", 200);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Par.K_ID, 2) //
        .with(EM.Par.MODEL_ID, MultivariateGaussianModelFactory.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 1.);
    assertClusterSizes(result, new int[] { 100, 100 });
  }

  @Test
  public void testConstantMAP() {
    Database db = makeSimpleDatabase(UNITTEST + "constant-attribute.csv.gz", 200);
    Clustering<?> result = new ELKIBuilder<EM<DoubleVector, ?>>(EM.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(EM.Par.K_ID, 2) //
        .with(EM.Par.MODEL_ID, MultivariateGaussianModelFactory.class) //
        .with(EM.Par.PRIOR_ID, .1) //
        .build().autorun(db);
    assertFMeasure(db, result, 1.);
    assertClusterSizes(result, new int[] { 100, 100 });
  }
}
