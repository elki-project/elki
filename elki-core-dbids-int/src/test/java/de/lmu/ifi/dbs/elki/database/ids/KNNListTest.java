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
package de.lmu.ifi.dbs.elki.database.ids;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test for KNNList.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KNNListTest {
  /**
   * Test that the knn distance for k &lt; kmax bug does come back.
   */
  @Test
  public void sublist() {
    double[] dists = { 1., 2., 4., 8., 4., 6., 2., 0., 6., 8., };
    DBIDRange range = DBIDFactory.FACTORY.generateStaticDBIDRange(dists.length);
    KNNHeap heap = DBIDUtil.newHeap(7);
    for(DBIDArrayIter it = range.iter(); it.valid(); it.advance()) {
      heap.insert(dists[it.getOffset()], it);
    }
    assertEquals("Tie @7 not handled correctly.", 8, heap.size());
    assertEquals("7NN distance wrong", 6., heap.getKNNDistance(), 0.);
    KNNList list = heap.toKNNList();
    assertEquals("Tie @7 not handled correctly.", 8, list.size());
    assertEquals("7NN distance wrong", 6., list.getKNNDistance(), 0.);
    assertEquals("Tie @5 not handled correctly.", 6, list.subList(5).size());
    assertEquals("6NN distance wrong", 4., list.subList(6).getKNNDistance(), 0.);
    assertEquals("5NN distance wrong", 4., list.subList(5).getKNNDistance(), 0.);
    assertEquals("4NN distance wrong", 2., list.subList(4).getKNNDistance(), 0.);
    assertEquals("3NN distance wrong", 2., list.subList(3).getKNNDistance(), 0.);
    assertEquals("2NN distance wrong", 1., list.subList(2).getKNNDistance(), 0.);
    assertEquals("1NN distance wrong", 0., list.subList(1).getKNNDistance(), 0.);
    for(DoubleDBIDListIter it = list.iter(); it.valid(); it.advance()) {
      assertEquals("Distance wrong @" + it.getOffset(), dists[range.getOffset(it)], it.doubleValue(), 0.);
    }
  }
}
