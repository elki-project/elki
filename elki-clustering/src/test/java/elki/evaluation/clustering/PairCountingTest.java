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
package elki.evaluation.clustering;

import static org.junit.Assert.*;

import org.junit.Test;

import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;

public class PairCountingTest {

  @Test
  public void testPairCountingSKlearn() {
    int[] a = { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3 };
    int[] b = { 1, 1, 1, 1, 2, 1, 2, 2, 2, 2, 3, 1, 3, 3, 3, 2, 2 };
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(a.length);
    PairCounting pc = new ClusterContingencyTable(false, false, EntropyTest.makeClustering(ids.iter(), a), EntropyTest.makeClustering(ids.iter(), b)).getPaircount();

    assertEquals("Precision not as expected", 0.476190476190476, pc.precision(), 1e-15);
    assertEquals("Recall not as expected", 0.5, pc.recall(), 1e-15);
    assertEquals("Rand-Index not as expected", 0.691176470588235, pc.randIndex(), 1e-15);
    assertEquals("Fowlkes-Mallows not as expected", 0.487950036474267, pc.fowlkesMallows(), 1e-15);
    assertEquals("Jaccard not as expected", 0.32258064516129, pc.jaccard(), 1e-15);
    assertEquals("F1-Measure not as expected", 0.487804878048781, pc.f1Measure(), 1e-15);
    assertEquals("Fb(5)-Measure not as expected", 0.499040307101727, pc.fMeasure(5), 1e-15);
    assertEquals("Adjusted Rand-Index not as expected", 0.26694045174538, pc.adjustedRandIndex(), 1e-15);
    assertEquals("Mirkin-Index not as expected", 168, pc.mirkin());
  }

}
