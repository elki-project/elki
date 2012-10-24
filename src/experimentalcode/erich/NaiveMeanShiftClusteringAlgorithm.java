package experimentalcode.erich;

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
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.statistics.EpanechnikovKernelDensityFunction;
import de.lmu.ifi.dbs.elki.math.statistics.KernelDensityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Mean-shift clustering algorithm. Naive implementation.
 * 
 * TODO: add methods to automatically choose the bandwidth?
 * 
 * TODO: what is the most appropriate reference for this?
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
public class NaiveMeanShiftClusteringAlgorithm<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, Clustering<MeanModel<V>>> implements ClusteringAlgorithm<Clustering<MeanModel<V>>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NaiveMeanShiftClusteringAlgorithm.class);

  /**
   * Density estimation kernel.
   */
  KernelDensityFunction kernel = EpanechnikovKernelDensityFunction.KERNEL;

  /**
   * Range of the kernel.
   */
  D range;

  /**
   * Maximum number of iterations.
   */
  static final int MAXITER = 1000;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param kernel Kernel function
   * @param range Kernel radius
   */
  public NaiveMeanShiftClusteringAlgorithm(DistanceFunction<? super V, D> distanceFunction, KernelDensityFunction kernel, D range) {
    super(distanceFunction);
    this.kernel = kernel;
    this.range = range;
  }

  /**
   * Run the mean-shift clustering algorithm.
   * 
   * @param database Database
   * @param relation Data relation
   * @return Clustering result
   */
  public Clustering<MeanModel<V>> run(Database database, Relation<V> relation) {
    final DistanceQuery<V, D> distq = database.getDistanceQuery(relation, getDistanceFunction());
    final RangeQuery<V, D> rangeq = database.getRangeQuery(distq);
    final int dim = RelationUtil.dimensionality(relation);

    // Kernel bandwidth, for normalization
    final double bandwidth = range.doubleValue();
    // Stopping threshold
    final double threshold = bandwidth * 1E-10;

    // Result store:
    ArrayList<Pair<V, ModifiableDBIDs>> clusters = new ArrayList<Pair<V, ModifiableDBIDs>>();

    ModifiableDBIDs noise = DBIDUtil.newArray();
    
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Mean-shift clustering", relation.size(), LOG) : null;

    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      // Initial position:
      V position = relation.get(iter);
      iterations: for (int j = 1; j <= MAXITER; j++) {
        // Compute new position:
        V newvec = null;
        {
          DistanceDBIDResult<D> neigh = rangeq.getRangeForObject(position, range);
          boolean okay = (neigh.size() > 1) || (neigh.size() >= 1 && j > 1);
          if (okay) {
            Centroid newpos = new Centroid(dim);
            for (DistanceDBIDResultIter<D> niter = neigh.iter(); niter.valid(); niter.advance()) {
              final double weight = kernel.density(niter.getDistance().doubleValue() / bandwidth);
              newpos.put(relation.get(niter), weight);
            }
            newvec = newpos.toVector(relation);
            // TODO: detect 0 weight!
          }
          if (!okay) {
            noise.add(iter);
            break iterations;
          }
        }
        // Test if we are close to one of the known clusters:
        double bestd = Double.POSITIVE_INFINITY;
        Pair<V, ModifiableDBIDs> bestp = null;
        for (Pair<V, ModifiableDBIDs> pair : clusters) {
          final double merged = distq.distance(newvec, pair.first).doubleValue();
          if (merged < bestd) {
            bestd = merged;
            bestp = pair;
          }
        }
        // Check for convergence:
        D delta = distq.distance(position, newvec);
        if (bestd < 10 * threshold || bestd * 2 < delta.doubleValue()) {
          bestp.second.add(iter);
          break iterations;
        }
        if (j == MAXITER) {
          LOG.warning("No convergence after " + MAXITER + " iterations. Distance: " + delta.toString());
        }
        if (Double.isNaN(delta.doubleValue())) {
          LOG.warning("Encountered NaN distance. Invalid center vector? " + newvec.toString());
          break iterations;
        }
        if (j == MAXITER || delta.doubleValue() < threshold) {
          if (LOG.isDebuggingFine()) {
            LOG.debugFine("New cluster:" + newvec + " delta: " + delta + " threshold: " + threshold + " bestd: " + bestd);
          }
          ArrayModifiableDBIDs cids = DBIDUtil.newArray();
          cids.add(iter);
          clusters.add(new Pair<V, ModifiableDBIDs>(newvec, cids));
          break iterations;
        }
        position = newvec;
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
    }

    ArrayList<Cluster<MeanModel<V>>> cs = new ArrayList<Cluster<MeanModel<V>>>(clusters.size());
    for (Pair<V, ModifiableDBIDs> pair : clusters) {
      cs.add(new Cluster<MeanModel<V>>(pair.second, new MeanModel<V>(pair.first)));
    }
    if (noise.size() > 0) {
      cs.add(new Cluster<MeanModel<V>>(noise, true));
    }
    Clustering<MeanModel<V>> c = new Clustering<MeanModel<V>>("Mean-shift Clustering", "mean-shift-clustering", cs);
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
   * Parameterizer.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Parameter for kernel function.
     */
    public static final OptionID KERNEL_ID = OptionID.getOrCreateOptionID("meanshift.kernel", "Kernel function to use with mean-shift clustering.");

    /**
     * Parameter for kernel radius.
     */
    public static final OptionID RANGE_ID = OptionID.getOrCreateOptionID("meanshift.kernel-range", "Range of the kernel to use (aka: radius, bandwidth).");

    /**
     * Kernel function.
     */
    KernelDensityFunction kernel = EpanechnikovKernelDensityFunction.KERNEL;

    /**
     * Kernel radius.
     */
    D range;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<KernelDensityFunction> kernelP = new ObjectParameter<KernelDensityFunction>(KERNEL_ID, KernelDensityFunction.class, EpanechnikovKernelDensityFunction.class);
      if (config.grab(kernelP)) {
        kernel = kernelP.instantiateClass(config);
      }
      DistanceParameter<D> rangeP = new DistanceParameter<D>(RANGE_ID, distanceFunction);
      if (config.grab(rangeP)) {
        range = rangeP.getValue();
      }
    }

    @Override
    protected NaiveMeanShiftClusteringAlgorithm<V, D> makeInstance() {
      return new NaiveMeanShiftClusteringAlgorithm<V, D>(distanceFunction, kernel, range);
    }
  }
}
