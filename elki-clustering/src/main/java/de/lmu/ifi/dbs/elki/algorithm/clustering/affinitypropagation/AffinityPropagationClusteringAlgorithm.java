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
package de.lmu.ifi.dbs.elki.algorithm.clustering.affinitypropagation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.MutableProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Cluster analysis by affinity propagation.
 * <p>
 * Reference:
 * <p>
 * B. J. Frey, D. Dueck<br>
 * Clustering by Passing Messages Between Data Points<br>
 * Science Vol 315
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - - - AffinityPropagationInitialization
 *
 * @param <O> object type
 */
@Title("Affinity Propagation: Clustering by Passing Messages Between Data Points")
@Reference(title = "Clustering by Passing Messages Between Data Points", //
    authors = "B. J. Frey, D. Dueck", //
    booktitle = "Science Vol 315", //
    url = "https://doi.org/10.1126/science.1136800", //
    bibkey = "doi:10.1126/science.1136800")
public class AffinityPropagationClusteringAlgorithm<O> extends AbstractAlgorithm<Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AffinityPropagationClusteringAlgorithm.class);

  /**
   * Similarity initialization
   */
  AffinityPropagationInitialization<O> initialization;

  /**
   * Damping factor lambda.
   */
  double lambda = 0.5;

  /**
   * Terminate after 10 iterations with no changes.
   */
  int convergence = 10;

  /**
   * Maximum number of iterations.
   */
  int maxiter = 1000;

  /**
   * Constructor.
   *
   * @param initialization Similarity initialization
   * @param lambda Damping factor
   * @param convergence Termination threshold (Number of stable iterations)
   * @param maxiter Maximum number of iterations
   */
  public AffinityPropagationClusteringAlgorithm(AffinityPropagationInitialization<O> initialization, double lambda, int convergence, int maxiter) {
    super();
    this.initialization = initialization;
    this.lambda = lambda;
    this.convergence = convergence;
    this.maxiter = maxiter;
  }

  /**
   * Perform affinity propagation clustering.
   *
   * @param db Database
   * @param relation Relation
   * @return Clustering result
   */
  public Clustering<MedoidModel> run(Database db, Relation<O> relation) {
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    int[] assignment = new int[size];
    double[][] s = initialization.getSimilarityMatrix(db, relation, ids);
    double[][] r = new double[size][size];
    double[][] a = new double[size][size];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("Affinity Propagation Iteration", LOG) : null;
    MutableProgress aprog = LOG.isVerbose() ? new MutableProgress("Stable assignments", size + 1, LOG) : null;

    int inactive = 0;
    for(int iteration = 0; iteration < maxiter && inactive < convergence; iteration++) {
      // Update responsibility matrix:
      for(int i = 0; i < size; i++) {
        double[] ai = a[i], ri = r[i], si = s[i];
        // Find the two largest values (as initially maxk == i)
        double max1 = Double.NEGATIVE_INFINITY, max2 = Double.NEGATIVE_INFINITY;
        int maxk = -1;
        for(int k = 0; k < size; k++) {
          double val = ai[k] + si[k];
          if(val > max1) {
            max2 = max1;
            max1 = val;
            maxk = k;
          }
          else if(val > max2) {
            max2 = val;
          }
        }
        // With the maximum value known, update r:
        for(int k = 0; k < size; k++) {
          double val = si[k] - ((k != maxk) ? max1 : max2);
          ri[k] = ri[k] * lambda + val * (1. - lambda);
        }
      }
      // Update availability matrix
      for(int k = 0; k < size; k++) {
        // Compute sum of max(0, r_ik) for all i.
        // For r_kk, don't apply the max.
        double colposum = 0.;
        for(int i = 0; i < size; i++) {
          if(i == k || r[i][k] > 0.) {
            colposum += r[i][k];
          }
        }
        for(int i = 0; i < size; i++) {
          double val = colposum;
          // Adjust column sum by the one extra term.
          if(i == k || r[i][k] > 0.) {
            val -= r[i][k];
          }
          if(i != k && val > 0.) { // min
            val = 0.;
          }
          a[i][k] = a[i][k] * lambda + val * (1 - lambda);
        }
      }
      int changed = 0;
      for(int i = 0; i < size; i++) {
        double[] ai = a[i], ri = r[i];
        double max = Double.NEGATIVE_INFINITY;
        int maxj = -1;
        for(int j = 0; j < size; j++) {
          double v = ai[j] + ri[j];
          if(v > max || (i == j && v >= max)) {
            max = v;
            maxj = j;
          }
        }
        if(assignment[i] != maxj) {
          changed += 1;
          assignment[i] = maxj;
        }
      }
      inactive = (changed > 0) ? 0 : (inactive + 1);
      LOG.incrementProcessed(prog);
      if(aprog != null) {
        aprog.setProcessed(size - changed, LOG);
      }
    }
    if(aprog != null) {
      aprog.setProcessed(aprog.getTotal(), LOG);
    }
    LOG.setCompleted(prog);
    // Cluster map, by lead object
    Int2ObjectOpenHashMap<ModifiableDBIDs> map = new Int2ObjectOpenHashMap<>();
    DBIDArrayIter i1 = ids.iter();
    for(int i = 0; i1.valid(); i1.advance(), i++) {
      int c = assignment[i];
      // Add to cluster members:
      ModifiableDBIDs cids = map.get(c);
      if(cids == null) {
        cids = DBIDUtil.newArray();
        map.put(c, cids);
      }
      cids.add(i1);
    }
    // If we stopped early, the cluster lead might be in a different cluster.
    for(ObjectIterator<Int2ObjectOpenHashMap.Entry<ModifiableDBIDs>> iter = map.int2ObjectEntrySet().fastIterator(); iter.hasNext();) {
      Int2ObjectOpenHashMap.Entry<ModifiableDBIDs> entry = iter.next();
      final int key = entry.getIntKey();
      int targetkey = key;
      ModifiableDBIDs tids = null;
      // Chase arrows:
      while(tids == null && assignment[targetkey] != targetkey) {
        targetkey = assignment[targetkey];
        tids = map.get(targetkey);
      }
      if(tids != null && targetkey != key) {
        tids.addDBIDs(entry.getValue());
        iter.remove();
      }
    }

    Clustering<MedoidModel> clustering = new Clustering<>("Affinity Propagation Clustering", "ap-clustering");
    ModifiableDBIDs noise = DBIDUtil.newArray();
    for(ObjectIterator<Int2ObjectOpenHashMap.Entry<ModifiableDBIDs>> iter = map.int2ObjectEntrySet().fastIterator(); iter.hasNext();) {
      Int2ObjectOpenHashMap.Entry<ModifiableDBIDs> entry = iter.next();
      i1.seek(entry.getIntKey());
      if(entry.getValue().size() > 1) {
        MedoidModel mod = new MedoidModel(DBIDUtil.deref(i1));
        clustering.addToplevelCluster(new Cluster<>(entry.getValue(), mod));
      }
      else {
        noise.add(i1);
      }
    }
    if(noise.size() > 0) {
      MedoidModel mod = new MedoidModel(DBIDUtil.deref(noise.iter()));
      clustering.addToplevelCluster(new Cluster<>(noise, true, mod));
    }
    return clustering;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(initialization.getInputTypeRestriction());
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
   * @hidden
   *
   * @param <O> object type
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for the similarity matrix initialization
     */
    public static final OptionID INITIALIZATION_ID = new OptionID("ap.initialization", "Similarity matrix initialization..");

    /**
     * Parameter for the dampening factor.
     */
    public static final OptionID LAMBDA_ID = new OptionID("ap.lambda", "Dampening factor lambda. Usually 0.5 to 1.");

    /**
     * Parameter for the convergence factor.
     */
    public static final OptionID CONVERGENCE_ID = new OptionID("ap.convergence", "Number of stable iterations for convergence.");

    /**
     * Parameter for the convergence factor.
     */
    public static final OptionID MAXITER_ID = new OptionID("ap.maxiter", "Maximum number of iterations.");

    /**
     * Initialization function for the similarity matrix.
     */
    AffinityPropagationInitialization<O> initialization;

    /**
     * Dampening parameter.
     */
    double lambda = .5;

    /**
     * Number of stable iterations for convergence.
     */
    int convergence;

    /**
     * Maximum number of iterations.
     */
    int maxiter;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<AffinityPropagationInitialization<O>> param = new ObjectParameter<>(INITIALIZATION_ID, AffinityPropagationInitialization.class, DistanceBasedInitializationWithMedian.class);
      if(config.grab(param)) {
        initialization = param.instantiateClass(config);
      }
      final DoubleParameter lambdaP = new DoubleParameter(LAMBDA_ID, .5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(lambdaP)) {
        lambda = lambdaP.doubleValue();
      }
      final IntParameter convergenceP = new IntParameter(CONVERGENCE_ID, 15) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(convergenceP)) {
        convergence = convergenceP.intValue();
      }
      final IntParameter maxiterP = new IntParameter(MAXITER_ID, 1000);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected AffinityPropagationClusteringAlgorithm<O> makeInstance() {
      return new AffinityPropagationClusteringAlgorithm<>(initialization, lambda, convergence, maxiter);
    }
  }
}
