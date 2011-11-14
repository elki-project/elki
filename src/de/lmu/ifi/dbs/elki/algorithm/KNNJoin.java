package de.lmu.ifi.dbs.elki.algorithm;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNList;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Joins in a given spatial database to each object its k-nearest neighbors.
 * This algorithm only supports spatial databases based on a spatial index
 * structure.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <N> the type of node used in the spatial index structure
 * @param <E> the type of entry used in the spatial node
 */
@Title("K-Nearest Neighbor Join")
@Description("Algorithm to find the k-nearest neighbors of each object in a spatial database")
public class KNNJoin<V extends NumberVector<V, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDistanceBasedAlgorithm<V, D, DataStore<KNNList<D>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNJoin.class);

  /**
   * Parameter that specifies the k-nearest neighbors to be assigned, must be an
   * integer greater than 0. Default value: 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnjoin.k", "Specifies the k-nearest neighbors to be assigned.");

  /**
   * The k parameter
   */
  int k;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k k parameter
   */
  public KNNJoin(DistanceFunction<? super V, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Joins in the given spatial database to each object its k-nearest neighbors.
   * 
   * @throws IllegalStateException if not suitable {@link SpatialIndexTree} was
   *         found or the specified distance function is not an instance of
   *         {@link SpatialPrimitiveDistanceFunction}.
   */
  @SuppressWarnings("unchecked")
  public WritableDataStore<KNNList<D>> run(Database database, Relation<V> relation) throws IllegalStateException {
    if(!(getDistanceFunction() instanceof SpatialPrimitiveDistanceFunction)) {
      throw new IllegalStateException("Distance Function must be an instance of " + SpatialPrimitiveDistanceFunction.class.getName());
    }
    Collection<SpatialIndexTree<N, E>> indexes = ResultUtil.filterResults(database, SpatialIndexTree.class);
    if(indexes.size() != 1) {
      throw new AbortException("KNNJoin found " + indexes.size() + " spatial indexes, expected exactly one.");
    }
    // FIXME: Ensure were looking at the right relation!
    SpatialIndexTree<N, E> index = indexes.iterator().next();
    SpatialPrimitiveDistanceFunction<V, D> distFunction = (SpatialPrimitiveDistanceFunction<V, D>) getDistanceFunction();
    DistanceQuery<V, D> distq = database.getDistanceQuery(relation, distFunction);

    DBIDs ids = relation.getDBIDs();

    WritableDataStore<KNNHeap<D>> knnHeaps = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, KNNHeap.class);

    try {
      // data pages of s
      List<E> ps_candidates = index.getLeaves();
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress(this.getClass().getName(), relation.size(), logger) : null;
      IndefiniteProgress pageprog = logger.isVerbose() ? new IndefiniteProgress("Number of processed data pages", logger) : null;
      if(logger.isDebugging()) {
        logger.debugFine("# ps = " + ps_candidates.size());
      }
      // data pages of r
      List<E> pr_candidates = new ArrayList<E>(ps_candidates);
      if(logger.isDebugging()) {
        logger.debugFine("# pr = " + pr_candidates.size());
      }
      int processed = 0;
      int processedPages = 0;
      boolean up = true;
      for(E pr_entry : pr_candidates) {
        N pr = index.getNode(pr_entry);
        D pr_knn_distance = distq.infiniteDistance();
        if(logger.isDebugging()) {
          logger.debugFine(" ------ PR = " + pr);
        }
        // create for each data object a knn list
        for(int j = 0; j < pr.getNumEntries(); j++) {
          knnHeaps.put(((LeafEntry) pr.getEntry(j)).getDBID(), new KNNHeap<D>(k, distq.infiniteDistance()));
        }

        if(up) {
          for(E ps_entry : ps_candidates) {
            D distance = distFunction.minDist(pr_entry, ps_entry);

            if(distance.compareTo(pr_knn_distance) <= 0) {
              N ps = index.getNode(ps_entry);
              pr_knn_distance = processDataPages(distq, pr, ps, knnHeaps);
            }
          }
          up = false;
        }
        else {
          for(int s = ps_candidates.size() - 1; s >= 0; s--) {
            E ps_entry = ps_candidates.get(s);
            D distance = distFunction.minDist(pr_entry, ps_entry);

            if(distance.compareTo(pr_knn_distance) <= 0) {
              N ps = index.getNode(ps_entry);
              pr_knn_distance = processDataPages(distq, pr, ps, knnHeaps);
            }
          }
          up = true;
        }

        processed += pr.getNumEntries();

        if(progress != null && pageprog != null) {
          progress.setProcessed(processed, logger);
          pageprog.setProcessed(processedPages++, logger);
        }
      }
      if(pageprog != null) {
        pageprog.setCompleted(logger);
      }
      WritableDataStore<KNNList<D>> knnLists = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, KNNList.class);
      for(DBID id : ids) {
        knnLists.put(id, knnHeaps.get(id).toKNNList());
      }
      return knnLists;
    }

    catch(Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Processes the two data pages pr and ps and determines the k-nearest
   * neighbors of pr in ps.
   * 
   * @param distQ the distance to use
   * @param pr the first data page
   * @param ps the second data page
   * @param knnLists the knn lists for each data object
   * @param pr_knn_distance the current knn distance of data page pr
   * @return the k-nearest neighbor distance of pr in ps
   */
  private D processDataPages(DistanceQuery<V, D> distQ, N pr, N ps, WritableDataStore<KNNHeap<D>> knnLists) {
    D pr_knn_distance = null;    
    // TODO: optimize for double?
    for(int i = 0; i < pr.getNumEntries(); i++) {
      DBID r_id = ((LeafEntry) pr.getEntry(i)).getDBID();
      KNNHeap<D> knnList = knnLists.get(r_id);

      for(int j = 0; j < ps.getNumEntries(); j++) {
        DBID s_id = ((LeafEntry) ps.getEntry(j)).getDBID();

        D distance = distQ.distance(r_id, s_id);
        knnList.add(distance, s_id);
      }
      // set kNN distance of r
      if(pr_knn_distance == null) {
        pr_knn_distance = knnList.getKNNDistance();
      }
      else {
        pr_knn_distance = DistanceUtil.max(knnList.getKNNDistance(), pr_knn_distance);
      }
    }
    return pr_knn_distance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<V, D> {
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, 1);
      kP.addConstraint(new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNJoin<V, D, N, E> makeInstance() {
      return new KNNJoin<V, D, N, E>(distanceFunction, k);
    }
  }
}