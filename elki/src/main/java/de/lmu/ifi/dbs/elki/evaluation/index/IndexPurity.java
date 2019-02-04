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
package de.lmu.ifi.dbs.elki.evaluation.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.index.tree.Node;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Compute the purity of index pages as a naive measure for performance
 * capabilities using the Gini index.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class IndexPurity implements Evaluator {
  /**
   * Constructor.
   */
  public IndexPurity() {
    super();
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    Database database = ResultUtil.findDatabase(hier);
    final ArrayList<SpatialIndexTree<?, ?>> indexes = ResultUtil.filterResults(hier, newResult, SpatialIndexTree.class);
    if(indexes == null || indexes.isEmpty()) {
      return;
    }
    Relation<String> lblrel = DatabaseUtil.guessLabelRepresentation(database);
    for(SpatialIndexTree<?, ?> index : indexes) {
      List<? extends SpatialEntry> leaves = index.getLeaves();
      MeanVariance mv = new MeanVariance();
      for(SpatialEntry e : leaves) {
        SpatialDirectoryEntry leaf = (SpatialDirectoryEntry) e;
        Node<?> n = index.getNode(leaf.getPageID());

        final int total = n.getNumEntries();
        HashMap<String, Integer> map = new HashMap<>(total);
        for(int i = 0; i < total; i++) {
          DBID id = ((SpatialPointLeafEntry) n.getEntry(i)).getDBID();
          String label = lblrel.get(id);
          Integer val = map.get(label);
          if(val == null) {
            val = 1;
          }
          else {
            val += 1;
          }
          map.put(label, val);
        }
        double gini = 0.0;
        for(Entry<String, Integer> ent : map.entrySet()) {
          double rel = ent.getValue() / (double) total;
          gini += rel * rel;
        }
        mv.put(gini);
      }
      Collection<double[]> col = new ArrayList<>();
      col.add(new double[] { mv.getMean(), mv.getSampleStddev() });
      database.getHierarchy().add((Result) index, new CollectionResult<>("Gini coefficient of index", "index-gini", col));
    }
  }
}
