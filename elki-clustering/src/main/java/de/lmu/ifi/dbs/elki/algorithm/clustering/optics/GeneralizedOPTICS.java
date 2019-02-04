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
package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.QuickSelectDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;

/**
 * A trivial generalization of OPTICS that is not restricted to numerical
 * distances, and serves as a base for several other algorithms (HiCO, HiSC).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - produces - ClusterOrder
 *
 * @param <O> the type of objects handled by the algorithm
 * @param <R> the type of results in the cluster order
 */
public abstract class GeneralizedOPTICS<O, R extends ClusterOrder> extends AbstractAlgorithm<R> implements OPTICSTypeAlgorithm {
  /**
   * Constructor.
   */
  public GeneralizedOPTICS() {
    super();
  }

  /**
   * Run OPTICS on the database.
   *
   * @param db Database
   * @param relation Relation
   * @return Result
   */
  public abstract ClusterOrder run(Database db, Relation<O> relation);

  // Usually: return new Instance(db, relation).run();

  /**
   * Instance for processing a single data set.
   *
   * @author Erich Schubert
   */
  public abstract static class Instance<O, R> implements Comparator<DBIDRef> {
    /**
     * Holds a set of processed ids.
     */
    protected ModifiableDBIDs processedIDs;

    /**
     * Current list of candidates.
     */
    protected ArrayModifiableDBIDs candidates;

    /**
     * Predecessor storage.
     */
    protected WritableDBIDDataStore predecessor;

    /**
     * Reachability storage.
     */
    protected WritableDoubleDataStore reachability;

    /**
     * IDs to process.
     */
    DBIDs ids;

    /**
     * Progress for logging.
     */
    FiniteProgress progress;

    /**
     * Constructor for a single data set.
     *
     * @param db Database
     * @param relation Data relation
     */
    public Instance(Database db, Relation<O> relation) {
      ids = relation.getDBIDs();
      processedIDs = DBIDUtil.newHashSet(ids.size());
      candidates = DBIDUtil.newArray();
      predecessor = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT);
      reachability = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      progress = getLogger().isVerbose() ? new FiniteProgress("OPTICS", ids.size(), getLogger()) : null;
    }

    @Override
    public int compare(DBIDRef o1, DBIDRef o2) {
      return Double.compare(reachability.doubleValue(o2), reachability.doubleValue(o1));
    }

    /**
     * Process the data set.
     *
     * @return Cluster order result.
     */
    public R run() {
      final Logging LOG = getLogger();
      DBIDVar cur = DBIDUtil.newVar();
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        if(processedIDs.contains(iditer)) {
          continue;
        }
        initialDBID(iditer);
        processedIDs.add(iditer);
        expandDBID(iditer);
        LOG.incrementProcessed(progress);

        while(!candidates.isEmpty()) {
          int last = candidates.size() - 1;
          QuickSelectDBIDs.quickSelect(candidates, this, last);
          candidates.assignVar(last, cur);
          candidates.remove(last);
          processedIDs.add(cur);

          expandDBID(cur);
          LOG.incrementProcessed(progress);
        }
      }
      LOG.ensureCompleted(progress);
      return buildResult();
    }

    /**
     * Initialize for a new DBID.
     *
     * @param id Current object ID
     */
    abstract protected void initialDBID(DBIDRef id);

    /**
     * Add the current DBID to the cluster order, and expand its neighbors if
     * minPts and similar conditions are satisfied.
     *
     * @param id Current object ID
     */
    abstract protected void expandDBID(DBIDRef id);

    /**
     * Build the final result.
     *
     * @return Result
     */
    abstract protected R buildResult();

    /**
     * Get the class logger.
     *
     * @return Class logger
     */
    abstract protected Logging getLogger();
  }
}
