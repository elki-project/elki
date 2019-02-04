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
package de.lmu.ifi.dbs.elki.evaluation.clustering;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * This class will log simple statistics on the clusters detected, such as the
 * cluster sizes and the number of clusters.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - evaluates - Clustering
 */
public class LogClusterSizes implements Evaluator {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(LogClusterSizes.class);

  /**
   * Prefix for logged statistics.
   */
  public static final String PREFIX = LogClusterSizes.class.getName() + ".";

  /**
   * Log the cluster sizes of a clustering.
   *
   * @param c Clustering ot analyze
   */
  public static <C extends Model> void logClusterSizes(Clustering<C> c) {
    if(!LOG.isStatistics()) {
      return;
    }
    final List<Cluster<C>> clusters = c.getAllClusters();
    final int numc = clusters.size();
    LOG.statistics(new StringStatistic(PREFIX + "name", c.getLongName()));
    LOG.statistics(new LongStatistic(PREFIX + "clusters", numc));
    Hierarchy<Cluster<C>> h = c.getClusterHierarchy();
    int cnum = 0;

    for(Cluster<C> clu : clusters) {
      final String p = PREFIX + "cluster-" + cnum + ".";
      if(clu.getName() != null) {
        LOG.statistics(new StringStatistic(p + "name", clu.getName()));
      }
      LOG.statistics(new LongStatistic(p + "size", clu.size()));
      if(clu.isNoise()) {
        LOG.statistics(new StringStatistic(p + "noise", "true"));
      }
      if(h.numChildren(clu) > 0) {
        // TODO: this only works if we have cluster names!
        StringBuilder buf = new StringBuilder();
        for(It<Cluster<C>> it = h.iterChildren(clu); it.valid(); it.advance()) {
          if(buf.length() > 0) {
            buf.append(", ");
          }
          buf.append(it.get().getName());
        }
        LOG.statistics(new StringStatistic(p + "children", buf.toString()));
      }
      // TODO: also log parents?
      ++cnum;
    }
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs == null || crs.isEmpty()) {
      return;
    }
    for(Clustering<?> c : crs) {
      logClusterSizes(c);
    }
  }
}
