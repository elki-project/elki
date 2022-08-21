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
package elki.clustering.subspace;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test P3C on a simple test data set.
 * <p>
 * Note: both data sets are really beneficial for P3C, and with reasonably
 * chosen parameters, it works perfectly.
 * <p>
 * FIXME: Previously, these test would score perfect. Now we have one outlier!
 * But from visual inspection, this might be a true positive.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class P3CTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testP3CSimple() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);
    Clustering<?> result = new ELKIBuilder<>(P3C.class).build().autorun(db);
    assertFMeasure(db, result, .99800101);
    assertClusterSizes(result, new int[] { 1, 200, 399 });
  }

  @Test
  public void testP3COverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<?> result = new ELKIBuilder<>(P3C.class) //
        .with(P3C.Par.ALPHA_THRESHOLD_ID, 0.005)//
        .build().autorun(db);
    assertFMeasure(db, result, .99798);
    assertClusterSizes(result, new int[] { 2, 149, 300, 399 });
  }
}
