package experimentalcode.students.waase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.histograms.FlexiHistogram;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * 
 * @author Ernst Waas
 */
public class LMCLUS<V extends NumberVector<V, ?>> extends AbstractAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LMCLUS.class);

  private final static double NOT_FROM_ONE_CLUSTER_PROBABILITY = 0.2;

  private final static int BINS = 50;

  private final static int NOISE_SIZE = 20;

  private final static double SMALL_NUMBER = 0;

  /**
   * The current threshold value calculated by the findSeperation Method.
   */
  private final double sensitivityThreshold;

  private final int maxLMDim;

  private final int samplingLevel;

  /**
   * Constructor.
   * 
   * @param maxdim Maximum dimensionality
   * @param samplingLevel Sampling level
   * @param sensitivityThreshold Treshold
   */
  public LMCLUS(int maxdim, int samplingLevel, double sensitivityThreshold) {
    this.maxLMDim = maxdim;
    this.samplingLevel = samplingLevel;
    this.sensitivityThreshold = sensitivityThreshold;
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
   * @param d The database to operate on
   * @param relation
   * @return A Clustering Object containing all the clusters found by the
   *         algorithm.
   * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException
   */
  public Clustering<Model> run(Database database, Relation<V> relation) throws UnableToComplyException {
    Clustering<Model> ret = new Clustering<Model>("LMCLUS Clustering", "lmclus-clustering");
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Clustered objects", relation.size(), logger) : null;
    IndefiniteProgress cprogress = logger.isVerbose() ? new IndefiniteProgress("Clusters found", logger) : null;
    ModifiableDBIDs unclustered = DBIDUtil.newHashSet(relation.getDBIDs());
    int cnum = 0;
    while(unclustered.size() > NOISE_SIZE) {
      DBIDs current = unclustered;
      int lmDim = 1;
      for(int k = 1; k <= maxLMDim; k++) {
        // System.out.println("Current dim: " + k);
        // System.out.println("Sampling level:" + samplingLevel);
        while(true) {
          Separation separation = findSeparation(relation, current, k);
          if(separation.goodness <= sensitivityThreshold) {
            break;
          }
          ModifiableDBIDs subset = DBIDUtil.newArray(current.size());
          for(DBID id : current) {
            if(deviation(relation.get(id).getColumnVector().minusEquals(separation.originV), separation.basis) < separation.threshold) {
              subset.add(id);
            }
          }
          if(subset.size() < NOISE_SIZE) {
            break;
          }
          current = subset;
          lmDim = k;
          // System.out.println("Partition: " + subset.size());
        }
      }
      // No more clusters found
      if(current.size() < NOISE_SIZE || current == unclustered) {
        break;
      }
      // New cluster found
      // TODO: annotate cluster with dimensionality
      final Cluster<Model> cluster = new Cluster<Model>(current);
      cluster.setName("Cluster_" + cnum + "_" + lmDim);
      cnum++;
      ret.addCluster(cluster);
      // Remove from main working set.
      unclustered.removeDBIDs(current);
      if(progress != null) {
        progress.setProcessed(relation.size() - unclustered.size(), logger);
      }
      if(cprogress != null) {
        cprogress.setProcessed(cnum, logger);
      }
    }
    // Remaining objects are noise
    if(unclustered.size() > 0) {
      ret.addCluster(new Cluster<Model>(unclustered, true));
    }
    if(progress != null) {
      progress.setProcessed(relation.size(), logger);
      progress.ensureCompleted(logger);
    }
    if(cprogress != null) {
      cprogress.setCompleted(logger);
    }
    return ret;
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
   * @return the overall goodness of the separation. The values origin basis and
   *         threshold are returned indirectly over class variables.
   */
  private Separation findSeparation(Relation<V> relation, DBIDs currentids, int dimension) {
    Separation separation = new Separation();
    // determine the number of samples needed, to secure that with a specific
    // probability
    // in at least on sample every sampled point is from the same cluster.
    int samples = (int) Math.min(Math.log(NOT_FROM_ONE_CLUSTER_PROBABILITY) / (Math.log(1 - Math.pow((1.0d / samplingLevel), dimension))), (double) currentids.size());
    // System.out.println("Dimension: " + dimension);
    System.out.println("Number of samples: " + samples);
    Random r = new Random();
    int remaining_retries = 100;
    for(int i = 1; i <= samples; i++) {
      // System.out.println(i);
      DBIDs sample = DBIDUtil.randomSample(currentids, dimension + 1, r.nextLong());
      final Iterator<DBID> iter = sample.iterator();
      // Use first as origin
      DBID origin = iter.next();
      Vector originV = relation.get(origin).getColumnVector();
      // Build orthogonal basis from remainder
      Matrix basis;
      {
        List<Vector> vectors = new ArrayList<Vector>(sample.size() - 1);
        while(iter.hasNext()) {
          Vector vec = relation.get(iter.next()).getColumnVector();
          vectors.add(vec.minus(originV));
        }
        // generate orthogonal basis
        basis = generateOrthonormalBasis(vectors);
        if(basis == null) {
          // new sample has to be taken.
          i--;
          remaining_retries--;
          if(remaining_retries < 0) {
            throw new AbortException("Too many retries in sampling, and always a linear dependant data set.");
          }
          continue;
        }
      }
      // System.out.println("Basis: " + tempBasis);
      // Generate and fill a histogram.
      FlexiHistogram<Double, Double> histogram = FlexiHistogram.DoubleSumHistogram(BINS);
      for(DBID point : currentids) {
        // Skip sampled points
        if(sample.contains(point)) {
          continue;
        }
        Vector vec = relation.get(point).getColumnVector().minusEquals(originV);
        final double distance = deviation(vec, basis);
        histogram.aggregate(distance, 1.0);
      }
      // System.out.println("breakPoint");
      double[] th = evaluateThreshold(histogram); // evaluate threshold
      if(th[1] > separation.goodness) {
        separation.goodness = th[1];
        separation.threshold = th[0];
        separation.originV = originV;
        separation.basis = basis;
      }
    }
    System.out.println("Goodness:" + separation.goodness);
    return separation;
  }

  /**
   * This Method generates an orthonormal basis from a set of Vectors. It uses
   * the established Gram-Schmidt algorithm for orthonormalisation:
   * 
   * <PRE>
   * u_1 = v_1
   * u_k = v_k -proj_u1(v_k)...proj_u(k-1)(v_k);
   * 
   * Where proj_u(v) = <v,u>/<u,u> *u
   * </PRE>
   * 
   * @param vectors The set of vectors to generate the orthonormal basis from
   * @return the orthonormal basis generated by this method.
   * @throws RuntimeException if the given vectors are not linear independent.
   */
  private Matrix generateOrthonormalBasis(List<Vector> vectors) {
    Vector first = vectors.get(0);
    first = first.times(1.0 / first.euclideanLength());
    Matrix ret = new Matrix(first.getDimensionality(), vectors.size());
    ret.setColumnVector(0, first);
    for(int i = 1; i < vectors.size(); i++) {
      // System.out.println("Matrix:" + ret);
      Vector v_i = vectors.get(i);
      Vector u_i = v_i.copy();
      // System.out.println("Vector " + i + ":" + partialSol);
      for(int j = 0; j < i; j++) {
        Vector v_j = ret.getColumnVector(j);
        double f = v_i.scalarProduct(v_j) / v_j.scalarProduct(v_j);
        if(Double.isNaN(f)) {
          if(logger.isDebuggingFine()) {
            logger.debugFine("Zero vector encountered? " + v_j);
          }
          return null;
        }
        u_i.minusTimesEquals(v_j, f);
      }
      // check if the vectors weren't independent
      final double len_u_i = u_i.euclideanLength();
      if(len_u_i == 0.0) {
        if(logger.isDebuggingFine()) {
          logger.debugFine("Points not independent - no orthonormalization.");
        }
        return null;
      }
      // System.out.println("Vector " + i + ":" + partialSol);
      u_i.timesEquals(1 / len_u_i);
      ret.setColumnVector(i, u_i);
    }
    return ret;
  }

  /**
   * 
   * @param histogramm
   * @return
   */
  private double[] evaluateThreshold(FlexiHistogram<Double, Double> histogramm) {
    // histogramm.replace(threshold, threshold); // FIXME: ????
    // double ret = 0;
    int n = histogramm.getNumBins();
    double[] p1 = new double[n];
    double[] p2 = new double[n];
    double[] mu1 = new double[n];
    double[] mu2 = new double[n];
    double[] sigma1 = new double[n];
    double[] sigma2 = new double[n];
    double[] jt = new double[n - 1];
    Iterator<Pair<Double, Double>> forward = histogramm.iterator();
    Iterator<Pair<Double, Double>> backwards = histogramm.reverseIterator();
    backwards.next();
    p1[0] = forward.next().second;
    p2[n - 2] = backwards.next().second;
    mu1[0] = 0;
    mu2[n - 2] = (p2[n - 2] == 0 ? 0 : (n - 1));
    sigma1[0] = 0;
    sigma2[n - 2] = 0;
    for(int i = 1, j = n - 3; i <= n - 2; i++, j--) {
      double hi = forward.next().second;
      double hj = backwards.next().second;
      p1[i] = p1[i - 1] + hi;
      if(p1[i] != 0) {
        mu1[i] = ((mu1[i - 1] * p1[i - 1]) + (i * hi)) / p1[i];
        sigma1[i] = (p1[i - 1] * (sigma1[i - 1] + (mu1[i - 1] - mu1[i]) * (mu1[i - 1] - mu1[i])) + hi * (i - mu1[i]) * (i - mu1[i])) / p1[i];
      }
      else {
        mu1[i] = 0;
        sigma1[i] = 0;
      }

      p2[j] = p2[j + 1] + hj;
      if(p2[j] != 0) {
        mu2[j] = ((mu2[j + 1] * p2[j + 1]) + ((j + 1) * hj)) / p2[j];
        sigma2[j] = (p2[j + 1] * (sigma2[j + 1] + (mu2[j + 1] - mu2[j]) * (mu2[j + 1] - mu2[j])) + hj * (j + 1 - mu2[j]) * (j + 1 - mu2[j])) / p2[j];
      }
      else {
        mu2[j] = 0;
        sigma2[j] = 0;
      }
    }

    for(int i = 0; i < n - 1; i++) {
      if(p1[i] != 0 && p2[i] != 0 && sigma1[i] > SMALL_NUMBER && sigma2[i] > SMALL_NUMBER) {
        jt[i] = 1.0d + 2.0d * (p1[i] * Math.log(Math.sqrt(sigma1[i])) + p2[i] * Math.log(Math.sqrt(sigma2[i]))) - 2.0d * (p1[i] * Math.log(p1[i]) + p2[i] * Math.log(p2[i]));
      }
      else {
        jt[i] = -1;
      }
    }

    int min = 0;
    double devPrev = jt[1] - jt[0];
    double globalDepth = -1;
    double discriminability = -1;
    for(int i = 1; i < jt.length - 1; i++) {
      double devCur = jt[i + 1] - jt[i];
      // System.out.println(p1[i]);
      // System.out.println(jt[i + 1]);
      // System.out.println(jt[i]);
      // System.out.println(devCur);
      // Minimum found calculate depth
      if(devCur >= 0 && devPrev <= 0) {
        double localDepth = 0;
        int leftHeight = i;
        while(leftHeight >= 1 && jt[leftHeight - 1] >= jt[leftHeight]) {
          leftHeight--;
        }
        int rightHeight = i;
        while(rightHeight < jt.length - 1 && jt[rightHeight] <= jt[rightHeight + 1]) {
          rightHeight++;
        }

        if(jt[leftHeight] < jt[rightHeight]) {
          localDepth = jt[leftHeight] - jt[i];
        }
        else {
          localDepth = jt[rightHeight] - jt[i];
        }
        if(localDepth > globalDepth) {
          // System.out.println("Minimum");
          // System.out.println(localDepth);
          min = i;
          globalDepth = localDepth;
          discriminability = Math.abs(mu1[i] - mu2[i]) / (Math.sqrt(sigma1[i] - sigma2[i]));
          if(Double.isNaN(discriminability)) {
            discriminability = -1;
          }
          // System.out.println(discriminability);
        }
      }
    }
    // ret = min * histogramm.getBinsize();
    double goodness = globalDepth * discriminability;
    // System.out.println("GLobal goodness:" + goodness + ";" + globalDepth +
    // ";" + discriminability);
    return new double[] { min * histogramm.getBinsize(), goodness };
  }

  /**
   * Deviation from a manifold described by beta.
   * 
   * @param delta Delta from origin vector
   * @param beta Manifold
   * @return Deviation score
   */
  private double deviation(Vector delta, Matrix beta) {
    double a = delta.euclideanLength();
    double b = beta.transposeTimes(delta).euclideanLength();
    return (a * a) - (b * b);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  public class Separation {
    double goodness = Double.NEGATIVE_INFINITY;

    double threshold = Double.NEGATIVE_INFINITY;

    Matrix basis = null;

    Vector originV = null;
  }

  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    public static final OptionID MAXLM_ID = OptionID.getOrCreateOptionID("lmclus.maxLMDim", "Maximum linear manifold dimension to compute.");

    public static final OptionID SAMPLINGL_ID = OptionID.getOrCreateOptionID("lmclus.samplingLevel", "A number used to determine how many samples are taken.");

    public static final OptionID THRESHOLD_ID = OptionID.getOrCreateOptionID("lmclus.threshold", "Threshold to determine if a cluster was found.");

    private int maxdim;

    private int samplingLevel;

    private double treshold;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter maxLMDimP = new IntParameter(MAXLM_ID, new GreaterEqualConstraint(1));
      if(config.grab(maxLMDimP)) {
        maxdim = maxLMDimP.getValue();
      }
      IntParameter samplingLevelP = new IntParameter(SAMPLINGL_ID);
      if(config.grab(samplingLevelP)) {
        samplingLevel = samplingLevelP.getValue();
      }

      DoubleParameter sensivityThresholdP = new DoubleParameter(THRESHOLD_ID);
      if(config.grab(sensivityThresholdP)) {
        treshold = sensivityThresholdP.getValue();
      }
    }

    @Override
    protected LMCLUS<V> makeInstance() {
      return new LMCLUS<V>(maxdim, samplingLevel, treshold);
    }
  }
}