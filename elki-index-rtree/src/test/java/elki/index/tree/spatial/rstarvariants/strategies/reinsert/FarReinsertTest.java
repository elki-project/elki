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
package elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import org.junit.Test;

import elki.data.NumberVector;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.rstarvariants.query.EuclideanRStarTreeDistancePrioritySearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNSearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeSearcher;
import elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import elki.index.tree.spatial.rstarvariants.strategies.overflow.LimitedReinsertOverflowTreatment;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Test the split only overflow treatment.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class FarReinsertTest extends AbstractIndexStructureTest {
  @Test
  public void testRTreeAngTanLinearSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<RStarTreeFactory<NumberVector>>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(LimitedReinsertOverflowTreatment.Par.REINSERT_STRATEGY_ID, FarReinsert.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
