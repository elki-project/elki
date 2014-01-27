package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * A trivial generalization of OPTICS that is not restricted to numerical
 * distances, and serves as a base for several other algorithms (HiCO, HiSC).
 * 
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <E> the type of entries in the cluster order
 */
public abstract class GeneralizedOPTICS<O, E extends ClusterOrderEntry<E>> extends AbstractAlgorithm<ClusterOrderResult<E>> implements OPTICSTypeAlgorithm<E> {
  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = new OptionID("optics.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds a set of processed ids.
   */
  private ModifiableDBIDs processedIDs;

  /**
   * Constructor.
   * 
   * @param minpts Minpts value
   */
  public GeneralizedOPTICS(int minpts) {
    super();
    this.minpts = minpts;
  }

  /**
   * Run OPTICS on the database.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public ClusterOrderResult<E> run(Relation<O> relation) {
    final DBIDs ids = relation.getDBIDs();
    final int size = ids.size();
    final FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Generalized OPTICS", size, getLogger()) : null;

    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<E> clusterOrder = new ClusterOrderResult<>(relation.getDatabase(), ids, "OPTICS Clusterorder", "optics-clusterorder");

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(!processedIDs.contains(iditer)) {
        expandClusterOrder(clusterOrder, relation, DBIDUtil.deref(iditer), progress);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }

    return clusterOrder;
  }

  /**
   * OPTICS-function expandClusterOrder.
   * 
   * @param clusterOrder Cluster order result to expand
   * @param relation the data relation to run on
   * @param rangeQuery the range query to use
   * @param objectID the currently processed object
   * @param epsilon Epsilon range value
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrder(ClusterOrderResult<E> clusterOrder, Relation<O> relation, DBID objectID, FiniteProgress progress) {
    UpdatableHeap<E> heap = new UpdatableHeap<>();
    heap.add(makeSeedEntry(relation, objectID));

    while(!heap.isEmpty()) {
      final E current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      Collection<E> neighbors = getNeighborsForDBID(relation, current.getID());
      if(neighbors != null && neighbors.size() >= minpts) {
        for(E entry : neighbors) {
          if(processedIDs.contains(entry.getID())) {
            continue;
          }
          heap.add(entry);
        }
      }
      if(progress != null) {
        progress.setProcessed(processedIDs.size(), getLogger());
      }
    }
  }

  /**
   * Create the initial element to seed the algorithm.
   * 
   * @param relation Data relation
   * @param objectID Object ID
   * @return Seed element.
   */
  abstract protected E makeSeedEntry(Relation<O> relation, DBID objectID);

  /**
   * Compute the neighbors for the given DBID.
   * 
   * @param relation Data relation
   * @param id Current object ID
   * @return Neighbors
   */
  abstract protected Collection<E> getNeighborsForDBID(Relation<O> relation, DBID id);

  @Override
  public int getMinPts() {
    return minpts;
  }
}