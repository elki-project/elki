package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Generate sorted ID pairs for a {@link Cluster}.
 * 
 * @author Erich Schubert
 */
public class PairGeneratorSingleCluster extends PairSortedGenerator {
  /**
   * Ids in parent clusters
   */
  private int[] parentids;

  /**
   * ids in this cluster
   */
  private int[] thisids;

  /**
   * Position in first set
   */
  private int pos1;

  /**
   * Position in second set
   */
  private int pos2;

  /**
   * Generate pairs for a hierarchical cluster.
   * 
   * @param cluster Cluster
   * @param useHierarchical Use hierarchical mode
   */
  public PairGeneratorSingleCluster(Cluster<?> cluster, boolean useHierarchical) {
    // collect all parent clusters into a flat list.
    java.util.Vector<Cluster<?>> allparents = new java.util.Vector<Cluster<?>>();
    if(useHierarchical && cluster.isHierarchical()) {
      allparents.addAll(cluster.getParents());
      for(int i = 0; i < allparents.size(); i++) {
        for(Cluster<?> newc : allparents.get(i).getParents()) {
          if(!allparents.contains(newc)) {
            allparents.add(newc);
          }
        }
      }
    }

    // build int array for the cluster
    DBIDs cids = cluster.getIDs();
    thisids = new int[cids.size()];
    {
      int j = 0;
      for(DBID id : cids) {
        thisids[j] = id.getIntegerID();
        j++;
      }
      Arrays.sort(thisids);
    }
    // TODO: ensure there are no duplicate IDs?

    ModifiableDBIDs idsset = DBIDUtil.newHashSet(cids);
    for(Cluster<?> parent : allparents) {
      idsset.addAll(parent.getIDs().asCollection());
    }
    parentids = new int[idsset.size()];
    {
      int j = 0;
      for(DBID in : idsset) {
        parentids[j] = in.getIntegerID();
        j++;
      }
      Arrays.sort(parentids);
    }

    // initialize iterator.
    pos1 = 0;
    pos2 = 0;
    if(thisids.length > 0) {
      setCurrent(new IntIntPair(parentids[pos1], thisids[pos2]));
    }
  }

  /**
   * Advance iterator
   */
  @Override
  protected IntIntPair advance() {
    if(current() == null) {
      return null;
    }
    pos2++;
    if(pos2 >= thisids.length) {
      pos2 = 0;
      pos1++;
    }
    if(pos1 >= parentids.length) {
      return null;
    }
    else {
      return new IntIntPair(parentids[pos1], thisids[pos2]);
    }
  }
}