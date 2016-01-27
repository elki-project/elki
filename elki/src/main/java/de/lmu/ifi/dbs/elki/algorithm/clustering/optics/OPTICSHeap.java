package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The OPTICS algorithm for density-based hierarchical clustering.
 *
 * This implementation uses a heap.
 *
 * Reference:
 * <p>
 * M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander:<br />
 * OPTICS: Ordering Points to Identify the Clustering Structure. <br/>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @apiviz.composedOf Instance
 *
 * @param <O> the type of DatabaseObjects handled by the algorithm
 */
@Title("OPTICS: Density-Based Hierarchical Clustering")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander", //
title = "OPTICS: Ordering Points to Identify the Clustering Structure", //
booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
url = "http://dx.doi.org/10.1145/304181.304187")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS")
public class OPTICSHeap<O> extends AbstractOPTICS<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSHeap.class);

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public OPTICSHeap(DistanceFunction<? super O> distanceFunction, double epsilon, int minpts) {
    super(distanceFunction, epsilon, minpts);
  }

  @Override
  public ClusterOrder run(Database db, Relation<O> relation) {
    return new Instance(db, relation).run();
  }

  /**
   * Instance for processing a single data set.
   *
   * @author Erich Schubert
   */
  private class Instance {
    /**
     * Holds a set of processed ids.
     */
    private ModifiableDBIDs processedIDs;

    /**
     * Heap of candidates.
     */
    UpdatableHeap<OPTICSHeapEntry> heap;

    /**
     * Output cluster order.
     */
    ClusterOrder clusterOrder;

    /**
     * IDs to process.
     */
    private DBIDs ids;

    /**
     * Progress for logging.
     */
    FiniteProgress progress;

    /**
     * Range query.
     */
    RangeQuery<O> rangeQuery;

    /**
     * Constructor for a single data set.
     *
     * @param db Database
     * @param relation Data relation
     */
    public Instance(Database db, Relation<O> relation) {
      ids = relation.getDBIDs();
      processedIDs = DBIDUtil.newHashSet(ids.size());
      clusterOrder = new ClusterOrder(ids, "OPTICS Clusterorder", "optics-clusterorder");
      progress = LOG.isVerbose() ? new FiniteProgress("OPTICS", ids.size(), LOG) : null;
      DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());
      rangeQuery = db.getRangeQuery(dq, epsilon);
      heap = new UpdatableHeap<>();
    }

    /**
     * Process the data set.
     *
     * @return Cluster order result.
     */
    public ClusterOrder run() {
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        if(!processedIDs.contains(iditer)) {
          assert (heap.isEmpty());
          expandClusterOrder(iditer);
        }
      }
      LOG.ensureCompleted(progress);
      return clusterOrder;
    }

    /**
     * OPTICS-function expandClusterOrder.
     *
     * @param objectID the currently processed object
     */
    protected void expandClusterOrder(DBIDRef objectID) {
      ModifiableDoubleDBIDList neighbors = DBIDUtil.newDistanceDBIDList();
      DoubleDBIDListIter neighbor = neighbors.iter();
      heap.add(new OPTICSHeapEntry(DBIDUtil.deref(objectID), null, Double.POSITIVE_INFINITY));

      while(!heap.isEmpty()) {
        final OPTICSHeapEntry current = heap.poll();
        clusterOrder.add(current.objectID, current.reachability, current.predecessorID);
        processedIDs.add(current.objectID);

        neighbors.clear();
        rangeQuery.getRangeForDBID(current.objectID, epsilon, neighbors);
        if(neighbors.size() >= minpts) {
          neighbors.sort();
          final double coreDistance = neighbor.seek(minpts - 1).doubleValue();

          for(neighbor.seek(0); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reachability = MathUtil.max(neighbor.doubleValue(), coreDistance);
            heap.add(new OPTICSHeapEntry(DBIDUtil.deref(neighbor), current.objectID, reachability));
          }
        }
        LOG.incrementProcessed(progress);
      }
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractOPTICS.Parameterizer<O> {
    @Override
    protected OPTICSHeap<O> makeInstance() {
      return new OPTICSHeap<>(distanceFunction, epsilon, minpts);
    }
  }
}
