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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.CoreObjectsModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Generalized DBSCAN, density-based clustering with noise.
 * <p>
 * Reference:
 * <p>
 * Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu<br>
 * Density-Based Clustering in Spatial Databases:
 * The Algorithm GDBSCAN and Its Applications<br>
 * Data Mining and Knowledge Discovery, 1998.
 *
 * @author Erich Schubert
 * @author Arthur Zimek
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @has - - - Instance
 * @composed - - - CorePredicate
 * @composed - - - NeighborPredicate
 */
@Reference(authors = "Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu", //
    title = "Density-Based Clustering in Spatial Databases: The Algorithm GDBSCAN and Its Applications", //
    booktitle = "Data Mining and Knowledge Discovery", //
    url = "https://doi.org/10.1023/A:1009745219419", //
    bibkey = "DBLP:journals/datamine/SanderEKX98")
public class GeneralizedDBSCAN extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Get a logger for this algorithm
   */
  private static final Logging LOG = Logging.getLogger(GeneralizedDBSCAN.class);

  /**
   * The neighborhood predicate factory.
   */
  protected NeighborPredicate<?> npred;

  /**
   * The core predicate factory.
   */
  protected CorePredicate<?> corepred;

  /**
   * Track which objects are "core" objects.
   */
  protected boolean coremodel = false;

  /**
   * Constructor for parameterized algorithm.
   *
   * @param npred Neighbor predicate.
   * @param corepred Core point predicate.
   * @param coremodel Keep track of core points.
   */
  public GeneralizedDBSCAN(NeighborPredicate<?> npred, CorePredicate<?> corepred, boolean coremodel) {
    super();
    this.npred = npred;
    this.corepred = corepred;
    this.coremodel = coremodel;
    // Ignore the generic, we do a run-time test below:
    @SuppressWarnings("unchecked")
    CorePredicate<Object> cp = (CorePredicate<Object>) corepred;
    if(!cp.acceptsType(npred.getOutputType())) {
      throw new AbortException("Core predicate and neighbor predicate are not compatible.");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Clustering<Model> run(Database database) {
    // Ignore the generic, we do a run-time test below:
    CorePredicate<Object> cp = (CorePredicate<Object>) corepred;
    if(!cp.acceptsType(npred.getOutputType())) {
      throw new AbortException("Core predicate and neighbor predicate are not compatible.");
    }
    return new Instance<>(npred.instantiate(database), cp.instantiate(database), coremodel).run();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(npred.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Instance for a particular data set.
   *
   * @author Erich Schubert
   *
   * @composed - - - CorePredicate.Instance
   * @composed - - - NeighborPredicate.Instance
   */
  public static class Instance<T> {
    /**
     * Unprocessed IDs
     */
    protected static final int UNPROCESSED = 0;

    /**
     * Noise IDs
     */
    protected static final int NOISE = 1;

    /**
     * The neighborhood predicate
     */
    protected final NeighborPredicate.Instance<T> npred;

    /**
     * The core object property
     */
    protected final CorePredicate.Instance<? super T> corepred;

    /**
     * Track which objects are "core" objects.
     */
    protected boolean coremodel = false;

    /**
     * Full Constructor
     *
     * @param npred Neighborhood predicate
     * @param corepred Core object predicate
     * @param coremodel Keep track of core points.
     */
    public Instance(NeighborPredicate.Instance<T> npred, CorePredicate.Instance<? super T> corepred, boolean coremodel) {
      super();
      this.npred = npred;
      this.corepred = corepred;
      this.coremodel = coremodel;
    }

    /**
     * Run the actual GDBSCAN algorithm.
     *
     * @return Clustering result
     */
    public Clustering<Model> run() {
      final DBIDs ids = npred.getIDs();
      // Setup progress logging
      final FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Generalized DBSCAN Clustering", ids.size(), LOG) : null;
      final IndefiniteProgress clusprogress = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters found", LOG) : null;
      // (Temporary) store the cluster ID assigned.
      final WritableIntegerDataStore clusterids = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, UNPROCESSED);
      // Note: these are not exact, as objects may be stolen from noise.
      final IntArrayList clustersizes = new IntArrayList();
      clustersizes.add(0); // Unprocessed dummy value.
      clustersizes.add(0); // Noise counter.
      final ArrayModifiableDBIDs activeSet = DBIDUtil.newArray();

      // Implementation Note: using Integer objects should result in
      // reduced memory use in the HashMap!
      int clusterid = NOISE + 1;
      // Iterate over all objects in the database.
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        // Skip already processed ids.
        if(clusterids.intValue(id) != UNPROCESSED) {
          continue;
        }
        // Evaluate Neighborhood predicate
        final T neighbors = npred.getNeighbors(id);
        // Evaluate Core-Point predicate:
        if(corepred.isCorePoint(id, neighbors)) {
          LOG.incrementProcessed(clusprogress);
          clustersizes.add(expandCluster(id, clusterid, clusterids, neighbors, activeSet, progress));
          // start next cluster on next iteration.
          ++clusterid;
        }
        else {
          // otherwise, it's a noise point
          clusterids.putInt(id, NOISE);
          clustersizes.set(NOISE, clustersizes.getInt(NOISE) + 1);
        }
        // We've completed this element
        LOG.incrementProcessed(progress);
      }
      // Finish progress logging.
      LOG.ensureCompleted(progress);
      LOG.setCompleted(clusprogress);

      // Transform cluster ID mapping into a clustering result:
      ArrayModifiableDBIDs[] clusterlists = new ArrayModifiableDBIDs[clusterid];
      ArrayModifiableDBIDs[] corelists = coremodel ? new ArrayModifiableDBIDs[clusterid] : null;
      // add storage containers for clusters
      for(int i = 0; i < clustersizes.size(); i++) {
        clusterlists[i] = DBIDUtil.newArray(clustersizes.getInt(i));
        if(corelists != null) {
          corelists[i] = DBIDUtil.newArray(clustersizes.getInt(i));
        }
      }
      // do the actual inversion
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        // Negative values are non-core points:
        final int cid = clusterids.intValue(id);
        final int cluster = cid < 0 ? -cid : cid;
        clusterlists[cluster].add(id);
        if(corelists != null && cid > NOISE) {
          corelists[cluster].add(id);
        }
      }
      clusterids.destroy();

      Clustering<Model> result = new Clustering<>("GDBSCAN", "gdbscan-clustering");
      for(int cid = NOISE; cid < clusterlists.length; cid++) {
        boolean isNoise = (cid == NOISE);
        Model m = coremodel ? new CoreObjectsModel(corelists[cid]) : ClusterModel.CLUSTER;
        result.addToplevelCluster(new Cluster<Model>(clusterlists[cid], isNoise, m));
      }
      return result;
    }

    /**
     * Set-based expand cluster implementation.
     *
     * @param clusterid ID of the current cluster.
     * @param clusterids Current object to cluster mapping.
     * @param neighbors Neighbors acquired by initial getNeighbors call.
     * @param activeSet Set to manage active candidates.
     * @param progress Progress logging
     * @return cluster size
     */
    protected int expandCluster(final DBIDRef seed, final int clusterid, final WritableIntegerDataStore clusterids, final T neighbors, ArrayModifiableDBIDs activeSet, final FiniteProgress progress) {
      assert (activeSet.size() == 0);
      int clustersize = 1 + processCorePoint(seed, neighbors, clusterid, clusterids, activeSet);
      // run expandCluster as long as there is another seed
      final DBIDVar id = DBIDUtil.newVar();
      while(!activeSet.isEmpty()) {
        activeSet.pop(id);
        // Evaluate Neighborhood predicate
        final T newneighbors = npred.getNeighbors(id);
        // Evaluate Core-Point predicate
        if(corepred.isCorePoint(id, newneighbors)) {
          clustersize += processCorePoint(id, newneighbors, clusterid, clusterids, activeSet);
        }
        LOG.incrementProcessed(progress);
      }
      return clustersize;
    }

    /**
     * Process a single core point.
     *
     * @param seed Point to process
     * @param newneighbors New neighbors
     * @param clusterid Cluster to add to
     * @param clusterids Cluster assignment storage.
     * @param activeSet Active set of cluster seeds
     * @return Number of new points added to cluster
     */
    protected int processCorePoint(final DBIDRef seed, T newneighbors, final int clusterid, final WritableIntegerDataStore clusterids, ArrayModifiableDBIDs activeSet) {
      clusterids.putInt(seed, clusterid); // Core point now
      int clustersize = 0;
      // The recursion is unrolled into iteration over the active set.
      for(DBIDIter it = npred.iterDBIDs(newneighbors); it.valid(); it.advance()) {
        final int oldassign = clusterids.intValue(it);
        if(oldassign == UNPROCESSED) {
          activeSet.add(it);
        }
        else if(oldassign != NOISE) {
          continue; // Member of some cluster.
        }
        clustersize++;
        clusterids.putInt(it, -clusterid);
      }
      return clustersize;
    }
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for neighborhood predicate.
     */
    public static final OptionID NEIGHBORHOODPRED_ID = new OptionID("gdbscan.neighborhood", //
        "Neighborhood predicate for Generalized DBSCAN");

    /**
     * Parameter for core predicate.
     */
    public static final OptionID COREPRED_ID = new OptionID("gdbscan.core", //
        "Core point predicate for Generalized DBSCAN");

    /**
     * Flag to keep track of core points.
     */
    public static final OptionID COREMODEL_ID = new OptionID("gdbscan.core-model", //
        "Use a model that keeps track of core points. Needs more memory.");

    /**
     * Neighborhood predicate.
     */
    protected NeighborPredicate<?> npred = null;

    /**
     * Core point predicate.
     */
    protected CorePredicate<?> corepred = null;

    /**
     * Track which objects are "core" objects.
     */
    protected boolean coremodel = false;

    @Override
    protected void makeOptions(Parameterization config) {
      // Neighborhood predicate
      ObjectParameter<NeighborPredicate<?>> npredOpt = new ObjectParameter<>(NEIGHBORHOODPRED_ID, NeighborPredicate.class, EpsilonNeighborPredicate.class);
      if(config.grab(npredOpt)) {
        npred = npredOpt.instantiateClass(config);
      }

      // Core point predicate
      ObjectParameter<CorePredicate<?>> corepredOpt = new ObjectParameter<>(COREPRED_ID, CorePredicate.class, MinPtsCorePredicate.class);
      if(config.grab(corepredOpt)) {
        corepred = corepredOpt.instantiateClass(config);
      }
      if(npred != null && corepred != null) {
        // Ignore the generic, we do a run-time test below:
        @SuppressWarnings("unchecked")
        CorePredicate<Object> cp = (CorePredicate<Object>) corepred;
        if(!cp.acceptsType(npred.getOutputType())) {
          config.reportError(new WrongParameterValueException(corepredOpt, corepredOpt.getValueAsString(), "Neighbor predicate and core predicate are not compatible."));
        }
      }

      Flag coremodelOpt = new Flag(COREMODEL_ID);
      if(config.grab(coremodelOpt)) {
        coremodel = coremodelOpt.isTrue();
      }
    }

    @Override
    protected GeneralizedDBSCAN makeInstance() {
      return new GeneralizedDBSCAN(npred, corepred, coremodel);
    }
  }
}
