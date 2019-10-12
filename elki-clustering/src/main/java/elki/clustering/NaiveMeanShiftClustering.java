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
package elki.clustering;

import java.util.ArrayList;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.linearalgebra.Centroid;
import elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import elki.math.statistics.kernelfunctions.KernelDensityFunction;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.pairs.Pair;

/**
 * Mean-shift based clustering algorithm. Naive implementation: there does not
 * seem to be "the" mean-shift clustering algorithm, but it is a general
 * concept. For the naive implementation, mean-shift is applied to all objects
 * until they converge to other. This implementation is quite naive, and various
 * optimizations can be made.
 * <p>
 * It also is not really parameter-free: the kernel needs to be specified,
 * including a radius/bandwidth.
 * <p>
 * By using range queries, the algorithm does benefit from index structures!
 * <p>
 * TODO: add methods to automatically choose the bandwidth?
 * <p>
 * Reference:
 * <p>
 * Y. Cheng<br>
 * Mean shift, mode seeking, and clustering<br>
 * IEEE Transactions on Pattern Analysis and Machine Intelligence 17-8
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @param <V> Vector type
 */
@Reference(authors = "Y. Cheng", //
    title = "Mean shift, mode seeking, and clustering", //
    booktitle = "IEEE Transactions on Pattern Analysis and Machine Intelligence 17-8", //
    url = "https://doi.org/10.1109/34.400568", //
    bibkey = "DBLP:journals/pami/Cheng95")
public class NaiveMeanShiftClustering<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<NumberVectorDistance<? super V>, Clustering<MeanModel>> implements ClusteringAlgorithm<Clustering<MeanModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NaiveMeanShiftClustering.class);

  /**
   * Density estimation kernel.
   */
  KernelDensityFunction kernel = EpanechnikovKernelDensityFunction.KERNEL;

  /**
   * Range of the kernel.
   */
  double bandwidth;

  /**
   * Maximum number of iterations.
   */
  static final int MAXITER = 1000;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param kernel Kernel function
   * @param range Kernel radius
   */
  public NaiveMeanShiftClustering(NumberVectorDistance<? super V> distance, KernelDensityFunction kernel, double range) {
    super(distance);
    this.kernel = kernel;
    this.bandwidth = range;
  }

  /**
   * Run the mean-shift clustering algorithm.
   *
   * @param relation Data relation
   * @return Clustering result
   */
  public Clustering<MeanModel> run(Relation<V> relation) {
    final QueryBuilder<V> qb = new QueryBuilder<>(relation, distance);
    final RangeQuery<V> rangeq = qb.rangeQuery(bandwidth);
    final DistanceQuery<V> distq = qb.distanceQuery();
    final NumberVector.Factory<V> factory = RelationUtil.getNumberVectorFactory(relation);
    final int dim = RelationUtil.dimensionality(relation);

    // Stopping threshold
    final double threshold = bandwidth * 1E-10;

    // Result store:
    ArrayList<Pair<V, ModifiableDBIDs>> clusters = new ArrayList<>();
    ModifiableDBIDs noise = DBIDUtil.newArray();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Mean-shift clustering", relation.size(), LOG) : null;

    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      // Initial position:
      V position = relation.get(iter);
      iterations: for(int j = 1;; j++) {
        // Compute new position:
        V newvec = null;
        {
          DoubleDBIDList neigh = rangeq.getRangeForObject(position, bandwidth);
          boolean okay = (neigh.size() > 1) || (neigh.size() >= 1 && j > 1);
          if(okay) {
            Centroid newpos = new Centroid(dim);
            for(DoubleDBIDListIter niter = neigh.iter(); niter.valid(); niter.advance()) {
              final double weight = kernel.density(niter.doubleValue() / bandwidth);
              newpos.put(relation.get(niter), weight);
            }
            newvec = factory.newNumberVector(newpos.getArrayRef());
            // TODO: detect 0 weight!
          }
          if(!okay) {
            noise.add(iter);
            break iterations;
          }
        }
        // Test if we are close to one of the known clusters:
        double bestd = Double.POSITIVE_INFINITY;
        Pair<V, ModifiableDBIDs> bestp = null;
        for(Pair<V, ModifiableDBIDs> pair : clusters) {
          final double merged = distq.distance(newvec, pair.first);
          if(merged < bestd) {
            bestd = merged;
            bestp = pair;
          }
        }
        // Check for convergence:
        double delta = distq.distance(position, newvec);
        if(bestd < 10 * threshold || bestd * 2 < delta) {
          assert (bestp != null);
          bestp.second.add(iter);
          break iterations;
        }
        if(Double.isNaN(delta)) {
          LOG.warning("Encountered NaN distance. Invalid center vector? " + newvec.toString());
          break iterations;
        }
        if(j == MAXITER || delta < threshold) {
          if(j == MAXITER) {
            LOG.warning("No convergence after " + MAXITER + " iterations. Distance: " + delta);
          }
          if(LOG.isDebuggingFine()) {
            LOG.debugFine("New cluster:" + newvec + " delta: " + delta + " threshold: " + threshold + " bestd: " + bestd);
          }
          ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
          cids.add(iter);
          clusters.add(new Pair<V, ModifiableDBIDs>(newvec, cids));
          break iterations;
        }
        position = newvec;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    ArrayList<Cluster<MeanModel>> cs = new ArrayList<>(clusters.size());
    for(Pair<V, ModifiableDBIDs> pair : clusters) {
      cs.add(new Cluster<>(pair.second, new MeanModel(pair.first.toArray())));
    }
    if(noise.size() > 0) {
      cs.add(new Cluster<MeanModel>(noise, true));
    }
    Clustering<MeanModel> c = new Clustering<>(cs);
    Metadata.of(c).setLongName("Mean-shift Clustering");
    return c;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Par.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Par<NumberVectorDistance<? super V>> {
    /**
     * Parameter for kernel function.
     */
    public static final OptionID KERNEL_ID = new OptionID("meanshift.kernel", "Kernel function to use with mean-shift clustering.");

    /**
     * Parameter for kernel radius/range/bandwidth.
     */
    public static final OptionID RANGE_ID = new OptionID("meanshift.kernel-bandwidth", "Range of the kernel to use (aka: radius, bandwidth).");

    /**
     * Kernel function.
     */
    KernelDensityFunction kernel = EpanechnikovKernelDensityFunction.KERNEL;

    /**
     * Kernel radius.
     */
    double range;

    @Override
    public Class<?> getDistanceRestriction() {
      return NumberVectorDistance.class;
    }

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<KernelDensityFunction>(KERNEL_ID, KernelDensityFunction.class, EpanechnikovKernelDensityFunction.class) //
          .grab(config, x -> kernel = x);
      new DoubleParameter(RANGE_ID) //
          .grab(config, x -> range = x);
    }

    @Override
    public NaiveMeanShiftClustering<V> make() {
      return new NaiveMeanShiftClustering<>(distance, kernel, range);
    }
  }
}
