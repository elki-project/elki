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
package elki.evaluation.clustering;

import java.util.ArrayList;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Abstract class for testing cluster evaluation measures.
 * Contains test data and helper functions.
 *
 * @author Erich Schubert
 * @author Robert Gehde
 * @since 0.8.0
 */
public abstract class AbstractClusterEvaluationTest {
  // SKLEARN example
  protected static final int[] SKLEARNA = { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3 };

  protected static final int[] SKLEARNB = { 1, 1, 1, 1, 2, 1, 2, 2, 2, 2, 3, 1, 3, 3, 3, 2, 2 };

  // Example with same clustering
  protected static final int[] SAMEA = { 0, 0, 1, 1, 2, 2 };

  protected static final int[] SAMEB = { 2, 2, 1, 1, 0, 0 };

  /**
   * Helper, to generate a clustering from int[]
   *
   * @param iter DBID Iterator
   * @param a cluster numbers
   * @return Clustering
   */
  public static Clustering<Model> makeClustering(DBIDArrayIter iter, int[] a) {
    Int2ObjectOpenHashMap<ModifiableDBIDs> l = new Int2ObjectOpenHashMap<>();
    for(int i = 0; i < a.length; i++) {
      int j = a[i];
      ModifiableDBIDs cids = l.get(j);
      if(cids == null) {
        l.put(j, cids = DBIDUtil.newArray());
      }
      cids.add(iter.seek(i));
    }
    ArrayList<Cluster<Model>> clusters = new ArrayList<>(l.size());
    // Negative cluster numbers are noise.
    for(Int2ObjectMap.Entry<ModifiableDBIDs> e : l.int2ObjectEntrySet()) {
      clusters.add(new Cluster<>(e.getValue(), e.getIntKey() < 0));
    }
    return new Clustering<>(clusters);
  }

  /**
   * Repeats the data
   *
   * @param data data to repeat
   * @param times number of times to repeat the data
   * @return array with repeated data
   */
  public static int[] repeat(int[] data, int times) {
    int[] res = new int[data.length * times];
    for(int i = 0; i < times; i++) {
      System.arraycopy(data, 0, res, i * data.length, data.length);
    }
    return res;
  }
}
