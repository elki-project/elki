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
package elki.evaluation.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.ids.DBID;
import elki.database.relation.Relation;
import elki.evaluation.Evaluator;
import elki.index.tree.Node;
import elki.index.tree.spatial.SpatialDirectoryEntry;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.SpatialIndexTree;
import elki.index.tree.spatial.SpatialPointLeafEntry;
import elki.math.MeanVariance;
import elki.result.CollectionResult;
import elki.result.Metadata;
import elki.result.ResultUtil;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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
  public void processNewResult(Object newResult) {
    Database database = ResultUtil.findDatabase(newResult);
    final ArrayList<SpatialIndexTree<?, ?>> indexes = ResultUtil.filterResults(newResult, SpatialIndexTree.class);
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
        Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<>(total);
        for(int i = 0; i < total; i++) {
          DBID id = ((SpatialPointLeafEntry) n.getEntry(i)).getDBID();
          String label = lblrel.get(id);
          map.addTo(label, 1);
        }
        double gini = 0.0;
        for(IntIterator it = map.values().iterator(); it.hasNext();) {
          double rel = it.nextInt() / (double) total;
          gini += rel * rel;
        }
        mv.put(gini);
      }
      Collection<double[]> col = new ArrayList<>();
      col.add(new double[] { mv.getMean(), mv.getSampleStddev() });
      CollectionResult<double[]> result = new CollectionResult<>(col);
      Metadata.of(result).setLongName("Gini coefficient of index");
      Metadata.hierarchyOf(index).addChild(result);
    }
  }
}
