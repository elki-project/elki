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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.DoubleDynamicHistogram;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.DoubleHistogram;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.DoubleHistogram.Iter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.TooManyRetriesException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Linear manifold clustering in high dimensional spaces by stochastic search.
 * <p>
 * Reference:
 * <p>
 * R. Haralick, R. Harpaz<br>
 * Linear manifold clustering in high dimensional spaces by stochastic
 * search<br>
 * In: Pattern Recognition volume 40, Issue 10
 * <p>
 * Implementation note: the LMCLUS algorithm seems to lack good stopping
 * criterions. We can't entirely reproduce the good results from the original
 * publication, in particular not on noisy data. But the questionable parts are
 * as in the original publication, associated thesis and published source code.
 * The minimum cluster size however can serve as a hidden stopping criterion.
 *
 * @author Ernst Waas
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @composed - - - Separation
 */
@Reference(authors = "R. Haralick, R. Harpaz", //
    title = "Linear manifold clustering in high dimensional spaces by stochastic search", //
    booktitle = "Pattern Recognition volume 40, Issue 10", //
    url = "https://doi.org/10.1016/j.patcog.2007.01.020", //
    bibkey = "DBLP:journals/pr/HaralickH07")
public class LMCLUS extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LMCLUS.class);

  /**
   * Epsilon
   */
  private static final double LOG_NOT_FROM_ONE_CLUSTER_PROBABILITY = Math.log(0.2);

  /**
   * Histogram resolution
   */
  private static final int BINS = 50;

  /**
   * The current threshold value calculated by the findSeperation Method.
   */
  private final double sensitivityThreshold;

  /**
   * Maximum cluster dimensionality
   */
  private final int maxLMDim;

  /**
   * Minimum cluster size
   */
  private final int minsize;

  /**
   * Number of sampling rounds to find a good split
   */
  private final int samplingLevel;

  /**
   * Random factory
   */
  private final RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param maxdim Maximum dimensionality
   * @param minsize Minimum cluster size
   * @param samplingLevel Sampling level
   * @param sensitivityThreshold Threshold
   * @param rnd Random factory
   */
  public LMCLUS(int maxdim, int minsize, int samplingLevel, double sensitivityThreshold, RandomFactory rnd) {
    super();
    this.maxLMDim = maxdim;
    this.minsize = minsize;
    this.samplingLevel = samplingLevel;
    this.sensitivityThreshold = sensitivityThreshold;
    this.rnd = rnd;
  }

  /**
   * The main LMCLUS (Linear manifold clustering algorithm) is processed in this
   * method.
   *
   * <PRE>
   * The algorithm samples random linear manifolds and tries to find clusters in it.
   * It calculates a distance histogram searches for a threshold and partitions the
   * points in two groups the ones in the cluster and everything else.
   * Then the best fitting linear manifold is searched and registered as a cluster.
   * The process is started over until all points are clustered.
   * The last cluster should contain all the outliers. (or the whole data if no clusters have been found.)
   * For details see {@link LMCLUS}.
   * </PRE>
   *
   * @param database The database to operate on
   * @param relation Relation
   * @return Clustering result
   */
  public Clustering<Model> run(Database database, Relation<NumberVector> relation) {
    Clustering<Model> ret = new Clustering<>("LMCLUS Clustering", "lmclus-clustering");
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Clustered objects", relation.size(), LOG) : null;
    IndefiniteProgress cprogress = LOG.isVerbose() ? new IndefiniteProgress("Clusters found", LOG) : null;
    ModifiableDBIDs unclustered = DBIDUtil.newHashSet(relation.getDBIDs());
    Random r = rnd.getSingleThreadedRandom();

    final int maxdim = Math.min(maxLMDim, RelationUtil.dimensionality(relation));
    int cnum = 0;
    while(unclustered.size() > minsize) {
      DBIDs current = unclustered;
      int lmDim = 1;
      for(int k = 1; k <= maxdim; k++) {
        // Implementation note: this while loop is from the original publication
        // and the published LMCLUS source code. It doesn't make sense to me -
        // it is lacking a stop criterion other than "cluster is too small" and
        // "cluster is inseparable"! Additionally, there is good criterion for
        // stopping at the appropriate dimensionality either.
        while(true) {
          Separation separation = findSeparation(relation, current, k, r);
          // logger.verbose("k: " + k + " goodness: " + separation.goodness +
          // " threshold: " + separation.threshold);
          if(separation.goodness <= sensitivityThreshold) {
            break;
          }
          ModifiableDBIDs subset = DBIDUtil.newArray(current.size());
          for(DBIDIter iter = current.iter(); iter.valid(); iter.advance()) {
            if(deviation(minusEquals(relation.get(iter).toArray(), separation.originV), separation.basis) < separation.threshold) {
              subset.add(iter);
            }
          }
          // logger.verbose("size:"+subset.size());
          if(subset.size() < minsize) {
            break;
          }
          current = subset;
          lmDim = k;
          // System.out.println("Partition: " + subset.size());
        }
      }
      // No more clusters found
      if(current.size() < minsize || current == unclustered) {
        break;
      }
      // New cluster found
      // TODO: annotate cluster with dimensionality
      final Cluster<Model> cluster = new Cluster<>(current);
      cluster.setName("Cluster_" + lmDim + "d_" + cnum);
      cnum++;
      ret.addToplevelCluster(cluster);
      // Remove from main working set.
      unclustered.removeDBIDs(current);
      if(progress != null) {
        progress.setProcessed(relation.size() - unclustered.size(), LOG);
      }
      if(cprogress != null) {
        cprogress.setProcessed(cnum, LOG);
      }
    }
    // Remaining objects are noise
    if(unclustered.size() > 0) {
      ret.addToplevelCluster(new Cluster<>(unclustered, true));
    }
    if(progress != null) {
      progress.setProcessed(relation.size(), LOG);
      progress.ensureCompleted(LOG);
    }
    LOG.setCompleted(cprogress);
    return ret;
  }

  /**
   * Deviation from a manifold described by beta.
   *
   * @param delta Delta from origin vector
   * @param beta Manifold
   * @return Deviation score
   */
  private double deviation(double[] delta, double[][] beta) {
    final double a = squareSum(delta);
    final double b = squareSum(transposeTimes(beta, delta));

    return (a > b) ? FastMath.sqrt(a - b) : 0.;
  }

  /**
   * This method samples a number of linear manifolds an tries to determine
   * which the one with the best cluster is.
   *
   * <PRE>
   * A number of sample points according to the dimension of the linear manifold are taken.
   * The basis (B) and the origin(o) of the manifold are calculated.
   * A distance histogram using  the distance function ||x-o|| -||B^t*(x-o)|| is generated.
   * The best threshold is searched using the elevate threshold function.
   * The overall goodness of the threshold is determined.
   * The process is redone until a specific number of samples is taken.
   * </PRE>
   *
   * @param relation The vector relation
   * @param currentids Current DBIDs
   * @param dimension the dimension of the linear manifold to sample.
   * @param r Random generator
   * @return the overall goodness of the separation. The values origin basis and
   *         threshold are returned indirectly over class variables.
   */
  private Separation findSeparation(Relation<NumberVector> relation, DBIDs currentids, int dimension, Random r) {
    Separation separation = new Separation();
    // determine the number of samples needed, to secure that with a specific
    // probability
    // in at least on sample every sampled point is from the same cluster.
    int samples = (int) Math.min(LOG_NOT_FROM_ONE_CLUSTER_PROBABILITY / (FastMath.log1p(-FastMath.powFast(samplingLevel, -dimension))), (double) currentids.size());
    // System.out.println("Number of samples: " + samples);
    int remaining_retries = 100;
    for(int i = 1; i <= samples; i++) {
      DBIDs sample = DBIDUtil.randomSample(currentids, dimension + 1, r);
      final DBIDIter iter = sample.iter();
      // Use first as origin
      double[] originV = relation.get(iter).toArray();
      iter.advance();
      // Build orthogonal basis from remainder
      double[][] basis;
      {
        List<double[]> vectors = new ArrayList<>(sample.size() - 1);
        for(; iter.valid(); iter.advance()) {
          double[] vec = relation.get(iter).toArray();
          vectors.add(minusEquals(vec, originV));
        }
        // generate orthogonal basis
        basis = generateOrthonormalBasis(vectors);
        if(basis == null) {
          // new sample has to be taken.
          i--;
          if(--remaining_retries < 0) {
            throw new TooManyRetriesException("Too many retries in sampling, and always a linear dependant data set.");
          }
          continue;
        }
      }
      // Generate and fill a histogram.
      DoubleDynamicHistogram histogram = new DoubleDynamicHistogram(BINS);
      double w = 1.0 / currentids.size();
      for(DBIDIter iter2 = currentids.iter(); iter2.valid(); iter2.advance()) {
        // Skip sampled points
        if(sample.contains(iter2)) {
          continue;
        }
        double[] vec = minusEquals(relation.get(iter2).toArray(), originV);
        final double distance = deviation(vec, basis);
        histogram.increment(distance, w);
      }
      double[] th = findAndEvaluateThreshold(histogram); // evaluate threshold
      if(th[1] > separation.goodness) {
        separation.goodness = th[1];
        separation.threshold = th[0];
        separation.originV = originV;
        separation.basis = basis;
      }
    }
    return separation;
  }

  /**
   * This Method generates an orthonormal basis from a set of Vectors. It uses
   * the established Gram-Schmidt algorithm for orthonormalisation:
   *
   * <pre>
   * u_1 = v_1
   * u_k = v_k -proj_u1(v_k)...proj_u(k-1)(v_k);
   *
   * Where proj_u(v) = &lt;v,u&gt;/&lt;u,u&gt; *u
   * </pre>
   *
   * @param vectors The set of vectors to generate the orthonormal basis from
   * @return the orthonormal basis generated by this method.
   * @throws RuntimeException if the given vectors are not linear independent.
   */
  private double[][] generateOrthonormalBasis(List<double[]> vectors) {
    double[] first = normalizeEquals(vectors.get(0));
    double[][] ret = new double[first.length][vectors.size()];
    setCol(ret, 0, first);
    for(int i = 1; i < vectors.size(); i++) {
      // System.out.println("Matrix:" + ret);
      double[] v_i = vectors.get(i);
      double[] u_i = v_i.clone();
      // System.out.println("Vector " + i + ":" + partialSol);
      for(int j = 0; j < i; j++) {
        double[] v_j = getCol(ret, j); // Must have length 1!
        double f = transposeTimes(v_i, v_j); // / transposeTimes(v_j, v_j);
        if(Double.isNaN(f)) {
          if(LOG.isDebuggingFine()) {
            LOG.debugFine("Zero vector encountered? " + FormatUtil.format(v_j));
          }
          return null;
        }
        minusTimesEquals(u_i, v_j, f);
      }
      // check if the vectors weren't independent
      final double len_u_i = euclideanLength(u_i);
      if(len_u_i < Double.MIN_NORMAL) {
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("Points not independent - no orthonormalization.");
        }
        return null;
      }
      timesEquals(u_i, 1 / len_u_i);
      setCol(ret, i, u_i);
    }
    return ret;
  }

  /**
   * Evaluate the histogram to find a suitable threshold
   *
   * @param histogram Histogram to evaluate
   * @return Position and goodness
   */
  private double[] findAndEvaluateThreshold(DoubleDynamicHistogram histogram) {
    int n = histogram.getNumBins();
    double[] p1 = new double[n];
    double[] p2 = new double[n];
    double[] mu1 = new double[n];
    double[] mu2 = new double[n];
    double[] sigma1 = new double[n];
    double[] sigma2 = new double[n];
    double[] jt = new double[n];
    // Forward pass
    {
      MeanVariance mv = new MeanVariance();
      DoubleHistogram.Iter forward = histogram.iter();
      for(int i = 0; forward.valid(); i++, forward.advance()) {
        p1[i] = forward.getValue() + ((i > 0) ? p1[i - 1] : 0);
        mv.put(i, forward.getValue());
        mu1[i] = mv.getMean();
        sigma1[i] = mv.getNaiveStddev();
      }
    }
    // Backwards pass
    {
      MeanVariance mv = new MeanVariance();
      DoubleHistogram.Iter backwards = histogram.iter();
      backwards.seek(histogram.getNumBins() - 1); // Seek to last

      for(int j = n - 1; backwards.valid(); j--, backwards.retract()) {
        p2[j] = backwards.getValue() + ((j + 1 < n) ? p2[j + 1] : 0);
        mv.put(j, backwards.getValue());
        mu2[j] = mv.getMean();
        sigma2[j] = mv.getNaiveStddev();
      }
    }

    for(int i = 0; i < n; i++) {
      jt[i] = 1.0 + 2 * (p1[i] * (FastMath.log(sigma1[i]) - FastMath.log(p1[i])) + p2[i] * (FastMath.log(sigma2[i]) - FastMath.log(p2[i])));
    }

    int bestpos = -1;
    double bestgoodness = Double.NEGATIVE_INFINITY;

    double devPrev = jt[1] - jt[0];
    for(int i = 1; i < jt.length - 1; i++) {
      double devCur = jt[i + 1] - jt[i];
      // Local minimum found - calculate depth
      if(devCur >= 0 && devPrev <= 0) {
        double lowestMaxima = Double.POSITIVE_INFINITY;
        for(int j = i - 1; j > 0; j--) {
          if(jt[j - 1] < jt[j]) {
            lowestMaxima = Math.min(lowestMaxima, jt[j]);
            break;
          }
        }
        for(int j = i + 1; j < n - 2; j++) {
          if(jt[j + 1] < jt[j]) {
            lowestMaxima = Math.min(lowestMaxima, jt[j]);
            break;
          }
        }
        double localDepth = lowestMaxima - jt[i];

        final double mud = mu1[i] - mu2[i];
        double discriminability = mud * mud / (sigma1[i] * sigma1[i] + sigma2[i] * sigma2[i]);
        if(Double.isNaN(discriminability)) {
          discriminability = -1;
        }
        double goodness = localDepth * discriminability;
        if(goodness > bestgoodness) {
          bestgoodness = goodness;
          bestpos = i;
        }
      }
      devPrev = devCur;
    }
    Iter iter = histogram.iter();
    iter.seek(bestpos);
    return new double[] { iter.getRight(), bestgoodness };
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Class to represent a linear manifold separation
   *
   * @author Erich Schubert
   */
  private static class Separation {
    /**
     * Goodness of separation
     */
    double goodness = Double.NEGATIVE_INFINITY;

    /**
     * Threshold
     */
    double threshold = Double.NEGATIVE_INFINITY;

    /**
     * Basis of manifold
     */
    double[][] basis = null;

    /**
     * Origin vector
     */
    double[] originV = null;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter with the maximum dimension to search for
     */
    public static final OptionID MAXDIM_ID = new OptionID("lmclus.maxdim", "Maximum linear manifold dimension to search.");

    /**
     * Parameter for the minimum cluster size
     */
    public static final OptionID MINSIZE_ID = new OptionID("lmclus.minsize", "Minimum cluster size to allow.");

    /**
     * Sampling intensity level
     */
    public static final OptionID SAMPLINGL_ID = new OptionID("lmclus.sampling-level", "A number used to determine how many samples are taken in each search.");

    /**
     * Global significance threshold
     */
    public static final OptionID THRESHOLD_ID = new OptionID("lmclus.threshold", "Threshold to determine if a cluster was found.");

    /**
     * Random seeding
     */
    public static final OptionID RANDOM_ID = new OptionID("lmclus.seed", "Random generator seed.");

    /**
     * Maximum dimensionality to search for
     */
    private int maxdim = Integer.MAX_VALUE;

    /**
     * Minimum cluster size.
     */
    private int minsize;

    /**
     * Sampling level
     */
    private int samplingLevel;

    /**
     * Threshold
     */
    private double threshold;

    /**
     * Random generator
     */
    private RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter maxLMDimP = new IntParameter(MAXDIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true);
      if(config.grab(maxLMDimP)) {
        maxdim = maxLMDimP.getValue();
      }
      IntParameter minsizeP = new IntParameter(MINSIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minsizeP)) {
        minsize = minsizeP.getValue();
      }
      IntParameter samplingLevelP = new IntParameter(SAMPLINGL_ID, 100);
      if(config.grab(samplingLevelP)) {
        samplingLevel = samplingLevelP.getValue();
      }
      DoubleParameter sensivityThresholdP = new DoubleParameter(THRESHOLD_ID);
      if(config.grab(sensivityThresholdP)) {
        threshold = sensivityThresholdP.getValue();
      }
      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected LMCLUS makeInstance() {
      return new LMCLUS(maxdim, minsize, samplingLevel, threshold, rnd);
    }
  }
}
