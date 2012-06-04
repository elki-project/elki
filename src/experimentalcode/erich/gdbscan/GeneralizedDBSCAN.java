package experimentalcode.erich.gdbscan;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Generalized DBSCAN, density-based clustering with noise.
 * <p>
 * Reference:<br />
 * Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu:<br />
 * Density-Based Clustering in Spatial Databases: The Algorithm GDBSCAN and Its
 * Applications<br />
 * In: Data Mining and Knowledge Discovery, 1998.
 * </p>
 * 
 * @author Erich Schubert
 * @author Arthur Zimek
 * 
 * @apiviz.has Instance
 */
@Reference(authors = "Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu", title = "Density-Based Clustering in Spatial Databases: The Algorithm GDBSCAN and Its Applications", booktitle = "Data Mining and Knowledge Discovery", url = "http://dx.doi.org/10.1023/A:1009745219419")
public class GeneralizedDBSCAN extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Get a logger for this algorithm
   */
  final static Logging logger = Logging.getLogger(GeneralizedDBSCAN.class);

  /**
   * The neighborhood predicate factory.
   */
  NeighborPredicate npred;

  /**
   * The core predicate factory.
   */
  CorePredicate corepred;

  /**
   * Constructor for parameterized algorithm.
   * 
   * @param npred Neighbor predicate
   * @param corepred Core point predicate
   */
  public GeneralizedDBSCAN(NeighborPredicate npred, CorePredicate corepred) {
    super();
    this.npred = npred;
    this.corepred = corepred;
  }

  @Override
  public Clustering<Model> run(Database database) {
    return new Instance(database, npred.instantiate(database), corepred.instantiate(database)).run();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(npred.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public class Instance {
    /**
     * The database context
     */
    final Database dbc;

    /**
     * The neighborhood predicate
     */
    final NeighborPredicate.Instance npred;

    /**
     * The core object property
     */
    final CorePredicate.Instance corepred;

    /**
     * Full Constructor
     * 
     * @param dbc Database context
     * @param npred Neighborhood predicate
     * @param corepred Core object predicate
     */
    public Instance(Database dbc, NeighborPredicate.Instance npred, CorePredicate.Instance corepred) {
      super();
      this.dbc = dbc;
      this.npred = npred;
      this.corepred = corepred;
    }

    /**
     * Run the actual DBSCAN algorithm.
     * 
     * @return Clustering result
     */
    public Clustering<Model> run() {
      final DBIDs ids = npred.getIDs();
      // Setup progress logging
      final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Clustering", ids.size(), logger) : null;
      final IndefiniteProgress clusprogress = logger.isVerbose() ? new IndefiniteProgress("Clusters", logger) : null;
      // (Temporary) store the cluster ID assigned.
      final WritableIntegerDataStore clusterids = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, -2);
      // Note: these are not exact!
      final TIntArrayList clustersizes = new TIntArrayList();
      // IDs to assign.

      // Implementation Note: using Integer objects should result in
      // reduced memory use in the HashMap!
      final int noiseid = -1;
      int clusterid = 0;
      int clustersize = 0;
      int noisesize = 0;
      // Iterate over all objects in the database.
      for(DBID id : ids) {
        // Skip already processed ids.
        if(clusterids.intValue(id) > -2) {
          continue;
        }
        // Evaluate Neighborhood predicate
        final DBIDs neighbors = npred.getNeighborDBIDs(id);
        // Evaluate Core-Point predicate:
        if(corepred.isCorePoint(id, neighbors)) {
          clusterids.putInt(id, clusterid);
          clustersize = 1 + setbasedExpandCluster(clusterid, clusterids, neighbors, progress);
          // start next cluster on next iteration.
          clustersizes.add(clustersize);
          clustersize = 0;
          clusterid += 1;
          if(clusprogress != null) {
            clusprogress.setProcessed(clusterid, logger);
          }
        }
        else {
          // otherwise, it's a noise point
          clusterids.putInt(id, noiseid);
          noisesize += 1;
        }
        // We've completed this element
        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
      // Finish progress logging.
      if(progress != null) {
        progress.ensureCompleted(logger);
      }
      if(clusprogress != null) {
        clusprogress.setCompleted(logger);
      }

      // Transform cluster ID mapping into a clustering result:
      ArrayList<ArrayModifiableDBIDs> clusterlists = new ArrayList<ArrayModifiableDBIDs>(clusterid + 1);
      // add noise cluster storage
      clusterlists.add(DBIDUtil.newArray(noisesize));
      // add storage containers for clusters
      for(int i = 0; i < clustersizes.size(); i++) {
        clusterlists.add(DBIDUtil.newArray(clustersizes.get(i)));
      }
      // do the actual inversion
      for(DBID id : ids) {
        int cluster = clusterids.intValue(id);
        clusterlists.get(cluster + 1).add(id);
      }
      clusterids.destroy();

      Clustering<Model> result = new Clustering<Model>("GDBSCAN", "gdbscan-clustering");
      int cid = 0;
      for(ArrayModifiableDBIDs res : clusterlists) {
        boolean isNoise = (cid == 0);
        Cluster<Model> c = new Cluster<Model>(res, isNoise, ClusterModel.CLUSTER);
        result.addCluster(c);
        cid++;
      }
      return result;
    }

    /**
     * Set-based expand cluster implementation.
     * 
     * @param clusterid ID of the current cluster.
     * @param clusterids Current object to cluster mapping.
     * @param neighbors Neighbors acquired by initial getNeighbors call.
     * @param progress Progress logging
     * 
     * @return cluster size;
     */
    protected int setbasedExpandCluster(final Integer clusterid, final WritableIntegerDataStore clusterids, final DBIDs neighbors, final FiniteProgress progress) {
      int clustersize = 0;
      // TODO: can we sometimes save this copy?
      final ArrayModifiableDBIDs activeSet = DBIDUtil.newArray(neighbors);
      // run expandCluster as long as this set is non-empty (non-recursive
      // implementation)
      while(!activeSet.isEmpty()) {
        final DBID id = activeSet.remove(activeSet.size() - 1);
        clustersize += 1;
        // Assign object to cluster
        final Integer oldclus = clusterids.putInt(id, clusterid);
        if(oldclus == null) {
          // expandCluster again:
          // Evaluate Neighborhood predicate
          final DBIDs newneighbors = npred.getNeighborDBIDs(id);
          // Evaluate Core-Point predicate
          if(corepred.isCorePoint(id, newneighbors)) {
            // Note: the recursion is unrolled into iteration over the active
            // set.
            activeSet.addDBIDs(newneighbors);
          }
          if(progress != null) {
            progress.incrementProcessed(logger);
          }
        }
      }
      return clustersize;
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Neighborhood predicate
     */
    NeighborPredicate npred = null;

    /**
     * Core point predicate
     */
    CorePredicate corepred = null;

    /**
     * Parameter for neighborhood predicate
     */
    public final static OptionID NEIGHBORHOODPRED_ID = OptionID.getOrCreateOptionID("gdbscan.neighborhood", "Neighborhood predicate for GDBSCAN");

    /**
     * Parameter for core predicate
     */
    public final static OptionID COREPRED_ID = OptionID.getOrCreateOptionID("gdbscan.core", "Core point predicate for GDBSCAN");

    @Override
    protected void makeOptions(Parameterization config) {
      // Neighborhood predicate
      ObjectParameter<NeighborPredicate> npredOpt = new ObjectParameter<NeighborPredicate>(NEIGHBORHOODPRED_ID, NeighborPredicate.class, EpsilonNeighborhood.class);
      if(config.grab(npredOpt)) {
        npred = npredOpt.instantiateClass(config);
      }

      // Core point predicate
      ObjectParameter<CorePredicate> corepredOpt = new ObjectParameter<CorePredicate>(COREPRED_ID, CorePredicate.class, MinPtsCorePredicate.class);
      if(config.grab(corepredOpt)) {
        corepred = corepredOpt.instantiateClass(config);
      }
    }

    @Override
    protected GeneralizedDBSCAN makeInstance() {
      return new GeneralizedDBSCAN(npred, corepred);
    }
  }
}