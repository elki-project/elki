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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.parallel;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.CorePredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.EpsilonNeighborPredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.MinPtsCorePredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.NeighborPredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.util.Assignment;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.util.Border;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.util.Core;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.util.MultiBorder;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.ParallelExecutor;
import de.lmu.ifi.dbs.elki.parallel.processor.Processor;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Parallel version of DBSCAN clustering.
 * <p>
 * This is the archetype of a non-linear shared-memory DBSCAN that does not
 * sequentially expand a cluster, but processes points in arbitrary order and
 * merges clusters when neighboring core points occur.
 * <p>
 * Because of synchronization when labeling points, the speedup will only be
 * sublinear in the number of cores. But in particular without an index and on
 * large data, the majority of the work is finding the neighbors; not in
 * labeling the points.
 * <p>
 * Reference:
 * <p>
 * Please cite the latest ELKI version.
 * <p>
 * Related is the following publication, whose "disjoint set data structure"
 * appears to be a similar union-find approach to ours, and whose DSDBSCAN
 * appears rather similar. The main benefit of our approach is that we avoid
 * using the union-find data structure for every object, but only use it for
 * merging clusters.
 * <p>
 * M. Patwary, D. Palsetia, A. Agrawal, W. K. Liao, F. Manne, A. Choudhary<br>
 * A new scalable parallel DBSCAN algorithm using the disjoint-set data
 * structure<br>
 * In IEEE Int. Conf. for High Performance Computing, Networking, Storage and
 * Analysis (SC)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @has - - - Instance
 * @composed - - - CorePredicate
 * @composed - - - NeighborPredicate
 */
@Reference(prefix = "closely related", //
    authors = "M. Patwary, D. Palsetia, A. Agrawal, W. K. Liao, F. Manne, A. Choudhary", //
    title = "A new scalable parallel DBSCAN algorithm using the disjoint-set data structure", //
    booktitle = "IEEE Int. Conf. for High Performance Computing, Networking, Storage and Analysis (SC)", //
    url = "https://doi.org/10.1109/SC.2012.9", //
    bibkey = "DBLP:conf/sc/PatwaryPALMC12")
public class ParallelGeneralizedDBSCAN extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Get a logger for this algorithm
   */
  private static final Logging LOG = Logging.getLogger(ParallelGeneralizedDBSCAN.class);

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
  public ParallelGeneralizedDBSCAN(NeighborPredicate<?> npred, CorePredicate<?> corepred, boolean coremodel) {
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
      throw new AbortException("Predicates are not compatible.");
    }
    return new Instance<>(database, npred, cp, coremodel).run();
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
  public static class Instance<T> implements Processor {
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
     * Cluster assignment storage.
     */
    private WritableDataStore<Assignment> clusterids;

    /**
     * Core objects (shared)
     */
    private Core[] cores;

    /**
     * Border objects (shared)
     */
    private Border[] borders;

    /**
     * Next cluster number assigned.
     */
    private int nextclus = 0;

    /**
     * Database for cloning neighbor predicates.
     */
    private Database database;

    /**
     * Factory for neighbor predicates.
     */
    private NeighborPredicate<? extends T> npreds;

    /**
     * Progress logger.
     */
    private FiniteProgress progress;

    /**
     * Full Constructor
     *
     * @param database Database to process
     * @param npreds Neighborhood predicate
     * @param corepred Core object predicate
     * @param coremodel Keep track of core points.
     */
    public Instance(Database database, NeighborPredicate<T> npreds, CorePredicate<? super T> corepred, boolean coremodel) {
      super();
      this.npred = npreds.instantiate(database);
      this.database = database;
      this.npreds = npreds;
      this.corepred = corepred.instantiate(database);
      this.coremodel = coremodel;
      this.clusterids = DataStoreUtil.makeStorage(npred.getIDs(), DataStoreFactory.HINT_TEMP, Assignment.class);
      this.cores = new Core[100];
      this.borders = new Border[100];
    }

    /**
     * Run the parallel GDBSCAN algorithm.
     *
     * @return Clustering result
     */
    public Clustering<Model> run() {
      DBIDs ids = npred.getIDs();

      progress = LOG.isVerbose() ? new FiniteProgress("DBSCAN clustering", ids.size(), LOG) : null;
      // Do the majority of the work in parallel:
      // (This will call "instantiate".)
      ParallelExecutor.run(ids, this);
      LOG.ensureCompleted(progress);

      // Build the final result
      FiniteProgress pprog = LOG.isVerbose() ? new FiniteProgress("Building final result", ids.size(), LOG) : null;
      ModifiableDBIDs[] clusters = new ModifiableDBIDs[nextclus];
      ModifiableDBIDs noise = DBIDUtil.newArray();
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        Assignment cids = clusterids.get(it);
        if(cids == null) {
          noise.add(it);
          LOG.incrementProcessed(pprog);
          continue;
        }
        if(cids instanceof MultiBorder) {
          cids = ((MultiBorder) cids).getCore();
        }
        else if(cids instanceof Border) {
          cids = ((Border) cids).core;
        }
        assert (cids instanceof Core);
        Core co = (Core) cids;
        while(cores[co.num].num != co.num) {
          co = cores[co.num = cores[co.num].num];
        }
        ModifiableDBIDs clu = clusters[co.num];
        if(clu == null) {
          clu = clusters[co.num] = DBIDUtil.newArray();
        }
        clu.add(it);
        LOG.incrementProcessed(pprog);
      }
      LOG.ensureCompleted(pprog);
      clusterids.destroy();

      // Wrap into final format
      Clustering<Model> result = new Clustering<>("DBSCAN Clustering", "dbscan-clustering");
      for(int i = 0; i < clusters.length; i++) {
        if(clusters[i] != null) {
          result.addToplevelCluster(new Cluster<Model>(clusters[i], ClusterModel.CLUSTER));
        }
      }
      if(noise.size() > 0) {
        result.addToplevelCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));
      }
      return result;
    }

    /**
     * Synchronize function to process the neighbors.
     *
     * @param id Current point
     * @param neighbors Neighbors
     */
    protected void processNeighbors(DBIDRef id, T neighbors) {
      if(!corepred.isCorePoint(id, neighbors)) {
        LOG.incrementProcessed(progress);
        return;
      }
      Core core = null;
      // Find a possible cluster assignment
      for(DBIDIter it = npred.iterDBIDs(neighbors); it.valid(); it.advance()) {
        Assignment cand = clusterids.get(it);
        if(cand instanceof Core) { // Another core point
          core = (Core) cand;
          break;
        }
      }
      synchronized(this) {
        // Make a new cluster
        if(core == null) {
          // Grow arrays when necessary:
          if(nextclus == cores.length) {
            cores = Arrays.copyOf(cores, cores.length + (cores.length >> 1));
            borders = Arrays.copyOf(borders, cores.length);
          }
          core = cores[nextclus] = new Core(nextclus);
          borders[nextclus] = new Border(core);
          ++nextclus;
        }
        Border border = borders[core.num];
        clusterids.put(id, core);
        // Label neighbors, execute merges.
        for(DBIDIter it = npred.iterDBIDs(neighbors); it.valid(); it.advance()) {
          Assignment oclus = clusterids.get(it);
          if(oclus == core) {
            continue;
          }
          if(oclus == null) { // Neighbor is still noise:
            clusterids.put(it, border);
          }
          else if(oclus instanceof Core) { // Core and core - merge!
            core.mergeWith((Core) oclus);
          }
          else if(oclus instanceof Border) { // Border point -> MultiBorder
            Border oborder = (Border) oclus;
            if(border.core.num != oborder.core.num) {
              clusterids.put(it, new MultiBorder(border, oborder));
            }
          }
          else { // Point is already border for multiple clusters
            assert (oclus instanceof MultiBorder);
            clusterids.put(it, ((MultiBorder) oclus).update(border));
          }
        }
      }
      LOG.incrementProcessed(progress);
    }

    @Override
    public Mapper instantiate(Executor executor) {
      NeighborPredicate.Instance<? extends T> inst = npreds.instantiate(database);
      return new Mapper(inst);
    }

    @Override
    public void cleanup(Processor.Instance inst) {
      // Nothing to do.
    }

    /**
     * Instance to process part of the data set, for a single iteration.
     *
     * @author Erich Schubert
     */
    private class Mapper implements Processor.Instance {
      /**
       * Neighbor predicate.
       */
      NeighborPredicate.Instance<? extends T> predicate;

      /**
       * Constructor.
       *
       * @param predicate Predicate to apply
       */
      public Mapper(NeighborPredicate.Instance<? extends T> predicate) {
        this.predicate = predicate;
      }

      @Override
      public void map(DBIDRef id) {
        processNeighbors(id, predicate.getNeighbors(id));
      }
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
    protected ParallelGeneralizedDBSCAN makeInstance() {
      return new ParallelGeneralizedDBSCAN(npred, corepred, coremodel);
    }
  }
}
