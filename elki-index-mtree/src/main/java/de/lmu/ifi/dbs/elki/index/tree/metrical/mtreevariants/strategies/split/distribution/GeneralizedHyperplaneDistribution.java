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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Generalized hyperplane entry distribution strategy of the M-tree.
 * <p>
 * This strategy does not produce balanced trees, but often produces faster
 * access times, according to the original publication.
 * <p>
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br>
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br>
 * In Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF", //
    bibkey = "DBLP:conf/vldb/CiacciaPZ97")
public class GeneralizedHyperplaneDistribution implements DistributionStrategy {
  @Override
  public <E extends MTreeEntry> Assignments<E> distribute(AbstractNode<E> node, int routing1, double[] dis1, int routing2, double[] dis2) {
    final int n = node.getNumEntries();
    assert dis1.length == n && dis2.length == n;
    final E e1 = node.getEntry(routing1), e2 = node.getEntry(routing2);
    Assignments<E> assign = new Assignments<>(e1.getRoutingObjectID(), e2.getRoutingObjectID(), n - 1);
    assign.addToFirst(e1, 0.);
    assign.addToSecond(e2, 0.);

    for(int i = 0, c1 = 1, c2 = 1; i < n; ++i) {
      if(i == routing1 || i == routing2) {
        continue;
      }
      final double d1 = dis1[i], d2 = dis2[i];
      if(d1 < d2 || (d1 == d2 && c1 < c2)) {
        assign.addToFirst(node.getEntry(i), d1);
        ++c1;
      }
      else {
        assign.addToSecond(node.getEntry(i), d2);
        ++c2;
      }
    }
    assert (assign.getFirstAssignments().size() + assign.getSecondAssignments().size() == n) : "Sizes do not sum up: " + assign.getFirstAssignments().size() + " + " + assign.getSecondAssignments().size() + " != " + n;
    return assign;
  }
}
