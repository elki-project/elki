/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2018
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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Generalized hyperplane entry distribution strategy of the M-tree.
 *
 * This strategy does not produce balanced trees, but often produces faster
 * access times, according to the original publication.
 *
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, and P. Zezula<br />
 * M-tree: An Efficient Access Method for Similarity Search in Metric
 * Spaces<br />
 * In Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 * </p>
 *
 * @author Erich Schubert
 */
@Reference(authors = "P. Ciaccia and M. Patella and P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF")
public class GeneralizedHyperplaneDistribution implements DistributionStrategy {
  @Override
  public <E extends MTreeEntry> Assignments<E> distribute(AbstractNode<E> node, int routing1, double[] dis1, int routing2, double[] dis2) {
    final int n = node.getNumEntries();
    assert dis1.length == n && dis2.length == n;
    final E e1 = node.getEntry(routing1), e2 = node.getEntry(routing2);
    Assignments<E> assign = new Assignments<>(e1.getRoutingObjectID(), e2.getRoutingObjectID(), (n + 1) >> 1);
    assign.addToFirst(e1, 0.);
    assign.addToSecond(e2, 0.);

    for(int p1 = 0, c1 = 0, c2 = 0; p1 < n; ++p1) {
      if(p1 == routing1 || p1 == routing2) {
        continue;
      }
      final double d1 = dis1[p1], d2 = dis2[p1];
      if(d1 < d2 || (d1 == d2 && c1 < c2)) {
        assign.addToFirst(node.getEntry(p1), d1);
        ++c1;
      }
      else {
        assign.addToSecond(node.getEntry(p1), d2);
        ++c2;
      }
    }
    assert (assign.getFirstAssignments().size() + assign.getSecondAssignments().size() == n) : "Sizes do not sum up: " + assign.getFirstAssignments().size() + " + " + assign.getFirstAssignments().size() + " != " + n;
    return assign;
  }
}
