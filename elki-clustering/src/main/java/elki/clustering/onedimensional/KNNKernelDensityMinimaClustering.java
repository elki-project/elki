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
package elki.clustering.onedimensional;

import elki.AbstractAlgorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.model.ClusterModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.StepProgress;
import elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import elki.math.statistics.kernelfunctions.KernelDensityFunction;
import elki.result.Metadata;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Cluster one-dimensional data by splitting the data set on local minima after
 * performing kernel density estimation.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class KNNKernelDensityMinimaClustering<V extends NumberVector> extends AbstractAlgorithm<Clustering<ClusterModel>> implements ClusteringAlgorithm<Clustering<ClusterModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KNNKernelDensityMinimaClustering.class);

  /**
   * Estimation mode.
   */
  public enum Mode {
    BALLOON, // Balloon estimator
    SAMPLE, // Sample-point estimator
  }

  /**
   * Dimension to use for clustering.
   */
  protected int dim;

  /**
   * Kernel density function.
   */
  protected KernelDensityFunction kernel;

  /**
   * Estimation modes.
   */
  protected Mode mode;

  /**
   * Number of neighbors to use for bandwidth.
   */
  protected int k;

  /**
   * Window width, for local minima criterions.
   */
  protected int minwindow;

  /**
   * Constructor.
   * 
   * @param dim Dimension to use for clustering
   * @param kernel Kernel function
   * @param mode Bandwidth mode
   * @param k Number of neighbors
   * @param minwindow Window size for comparison
   */
  public KNNKernelDensityMinimaClustering(int dim, KernelDensityFunction kernel, Mode mode, int k, int minwindow) {
    super();
    this.dim = dim;
    this.kernel = kernel;
    this.mode = mode;
    this.k = k;
    this.minwindow = minwindow;
  }

  /**
   * Run the clustering algorithm on a data relation.
   * 
   * @param relation Relation
   * @return Clustering result
   */
  public Clustering<ClusterModel> run(Relation<V> relation) {
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    final int size = ids.size();

    // Sort by the sole dimension
    ids.sort(new VectorUtil.SortDBIDsBySingleDimension(relation, dim));

    // Density storage.
    WritableDoubleDataStore density = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);

    DBIDArrayIter iter = ids.iter(), iter2 = ids.iter();

    StepProgress sprog = LOG.isVerbose() ? new StepProgress("Clustering steps", 2) : null;

    LOG.beginStep(sprog, 1, "Kernel density estimation.");
    {
      double[] scratch = new double[2 * k];
      iter.seek(0);
      for(int i = 0; i < size; i++, iter.advance()) {
        // Current value.
        final double curv = relation.get(iter).doubleValue(dim);

        final int pre = Math.max(i - k, 0), prek = i - pre;
        final int pos = Math.min(i + k, size - 1), posk = pos - i;
        iter2.seek(pre);
        for(int j = 0; j < prek; j++, iter2.advance()) {
          scratch[j] = curv - relation.get(iter2).doubleValue(dim);
        }
        assert (iter2.getOffset() == i);
        iter2.advance();
        for(int j = 0; j < posk; j++, iter2.advance()) {
          scratch[prek + j] = relation.get(iter2).doubleValue(dim) - curv;
        }

        assert (prek + posk >= k);
        double kdist = QuickSelect.quickSelect(scratch, 0, prek + posk, k);
        switch(mode){
        case BALLOON: {
          double dens = 0.;
          if(kdist > 0.) {
            for(int j = 0; j < prek + posk; j++) {
              dens += kernel.density(scratch[j] / kdist);
            }
          }
          else {
            dens = Double.POSITIVE_INFINITY;
          }
          assert (iter.getOffset() == i);
          density.putDouble(iter, dens);
          break;
        }
        case SAMPLE: {
          if(kdist > 0.) {
            iter2.seek(pre);
            for(int j = 0; j < prek; j++, iter2.advance()) {
              double delta = curv - relation.get(iter2).doubleValue(dim);
              density.putDouble(iter2, density.doubleValue(iter2) + kernel.density(delta / kdist));
            }
            assert (iter2.getOffset() == i);
            iter2.advance();
            for(int j = 0; j < posk; j++, iter2.advance()) {
              double delta = relation.get(iter2).doubleValue(dim) - curv;
              density.putDouble(iter2, density.doubleValue(iter2) + kernel.density(delta / kdist));
            }
          }
          else {
            iter2.seek(pre);
            for(int j = 0; j < prek; j++, iter2.advance()) {
              double delta = curv - relation.get(iter2).doubleValue(dim);
              if(!(delta > 0.)) {
                density.putDouble(iter2, Double.POSITIVE_INFINITY);
              }
            }
            assert (iter2.getOffset() == i);
            iter2.advance();
            for(int j = 0; j < posk; j++, iter2.advance()) {
              double delta = relation.get(iter2).doubleValue(dim) - curv;
              if(!(delta > 0.)) {
                density.putDouble(iter2, Double.POSITIVE_INFINITY);
              }
            }
          }
          break;
        }
        default:
          throw new UnsupportedOperationException("Unknown mode specified.");
        }
      }
    }

    LOG.beginStep(sprog, 2, "Local minima detection.");
    Clustering<ClusterModel> clustering = new Clustering<>();
    Metadata.of(clustering).setLongName("One-Dimensional KDE Clustering");
    {
      double[] scratch = new double[2 * minwindow + 1];
      int begin = 0;
      int halfw = (minwindow + 1) >> 1;
      iter.seek(0);
      // Fill initial buffer.
      for(int i = 0; i < size; i++, iter.advance()) {
        final int m = i % scratch.length, t = (i - minwindow - 1) % scratch.length;
        scratch[m] = density.doubleValue(iter);
        if(i > scratch.length) {
          double min = Double.POSITIVE_INFINITY;
          for(int j = 0; j < scratch.length; j++) {
            if(j != t && scratch[j] < min) {
              min = scratch[j];
            }
          }
          // Local minimum:
          if(scratch[t] < min) {
            int end = i - minwindow + 1;
            { // Test on which side the kNN is
              iter2.seek(end);
              double curv = relation.get(iter2).doubleValue(dim);
              iter2.seek(end - halfw);
              double left = relation.get(iter2).doubleValue(dim) - curv;
              iter2.seek(end + halfw);
              double right = curv - relation.get(iter2).doubleValue(dim);
              if(left < right) {
                end++;
              }
            }
            iter2.seek(begin);
            ArrayModifiableDBIDs cids = DBIDUtil.newArray(end - begin);
            for(int j = 0; j < end - begin; j++, iter2.advance()) {
              cids.add(iter2);
            }
            clustering.addToplevelCluster(new Cluster<>(cids, ClusterModel.CLUSTER));
            begin = end;
          }
        }
      }
      // Extract last cluster
      int end = size;
      iter2.seek(begin);
      ArrayModifiableDBIDs cids = DBIDUtil.newArray(end - begin);
      for(int j = 0; j < end - begin; j++, iter2.advance()) {
        cids.add(iter2);
      }
      clustering.addToplevelCluster(new Cluster<>(cids, ClusterModel.CLUSTER));
    }

    LOG.ensureCompleted(sprog);
    return clustering;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(VectorFieldTypeInformation.typeRequest(NumberVector.class, dim + 1, Integer.MAX_VALUE));
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Dimension to use for clustering.
     */
    public static final OptionID DIM_ID = new OptionID("kernelcluster.dim", "Dimension to use for clustering. For one-dimensional data, use 0.");

    /**
     * Kernel function.
     */
    public static final OptionID KERNEL_ID = new OptionID("kernelcluster.kernel", "Kernel function for density estimation.");

    /**
     * KDE mode.
     */
    public static final OptionID MODE_ID = new OptionID("kernelcluster.mode", "Kernel density estimation mode (baloon estimator vs. sample point estimator).");

    /**
     * Number of neighbors for bandwidth estimation.
     */
    public static final OptionID K_ID = new OptionID("kernelcluster.knn", "Number of nearest neighbors to use for bandwidth estimation.");

    /**
     * Half window width to find local minima.
     */
    public static final OptionID WINDOW_ID = new OptionID("kernelcluster.window", "Half width of sliding window to find local minima.");

    /**
     * Dimension to use for clustering.
     */
    protected int dim;

    /**
     * Kernel density function.
     */
    protected KernelDensityFunction kernel;

    /**
     * Estimation modes.
     */
    protected Mode mode;

    /**
     * Number of neighbors to use for bandwidth.
     */
    protected int k;

    /**
     * Window width, for local minima criterions.
     */
    protected int minwindow;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(DIM_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> dim = x);
      new ObjectParameter<KernelDensityFunction>(KERNEL_ID, KernelDensityFunction.class, EpanechnikovKernelDensityFunction.class) //
          .grab(config, x -> kernel = x);
      new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.BALLOON) //
          .grab(config, x -> mode = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new IntParameter(WINDOW_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minwindow = x);
    }

    @Override
    public KNNKernelDensityMinimaClustering<V> make() {
      return new KNNKernelDensityMinimaClustering<>(dim, kernel, mode, k, minwindow);
    }
  }
}
