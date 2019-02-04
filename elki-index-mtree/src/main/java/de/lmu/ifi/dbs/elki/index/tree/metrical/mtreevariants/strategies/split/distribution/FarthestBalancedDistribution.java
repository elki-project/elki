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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution;

import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

/**
 * Balanced entry distribution strategy of the M-tree, beginning with the most
 * difficult points first. This should produce smaller covers.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FarthestBalancedDistribution implements DistributionStrategy {
  @Override
  public <E extends MTreeEntry> Assignments<E> distribute(AbstractNode<E> node, int routing1, double[] dis1, int routing2, double[] dis2) {
    final int n = node.getNumEntries(), l = n - 2;
    assert dis1.length == n && dis2.length == n;
    int[] idx1 = new int[l];
    for(int i = 0, j = 0; i < n; i++) {
      if(i != routing1 && i != routing2) {
        idx1[j++] = i;
      }
    }

    // Descending, by max distance to routing objects:
    IntegerArrayQuickSort.sort(idx1, 0, l, (a, b) -> Double.compare(Math.max(dis1[b], dis2[b]), Math.max(dis1[a], dis2[a])));

    final E e1 = node.getEntry(routing1), e2 = node.getEntry(routing2);
    Assignments<E> assign = new Assignments<>(e1.getRoutingObjectID(), e2.getRoutingObjectID(), (n + 1) >> 1);
    assign.addToFirst(e1, 0.);
    assign.addToSecond(e2, 0.);

    final int h = (n + 1) >> 1;
    for(int p = 0, s1 = 1, s2 = 1; p < l; ++p) {
      int i = idx1[p];
      double d1 = dis1[i], d2 = dis2[i];
      if(s2 == h || (s1 != h && (d1 < d2 || (d1 == d2 && s1 < s2)))) {
        assign.addToFirst(node.getEntry(i), d1);
        s1++;
      }
      else {
        assign.addToSecond(node.getEntry(i), d2);
        s2++;
      }
    }
    assert (assign.getFirstAssignments().size() + assign.getSecondAssignments().size() == n) : "Sizes do not sum up: " + assign.getFirstAssignments().size() + " + " + assign.getSecondAssignments().size() + " != " + n;
    assert (Math.abs(assign.getFirstAssignments().size() - assign.getSecondAssignments().size()) <= 1) : "Not balanced: " + assign.getFirstAssignments().size() + " " + assign.getSecondAssignments().size();
    return assign;
  }
}
