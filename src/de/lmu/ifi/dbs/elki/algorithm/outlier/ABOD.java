package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.ABODResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.ComparablePair;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Angle-Based Outlier Detection
 * 
 * Outlier detection using variance analysis on angles, especially for high dimensional data sets.
 * 
 * H.-P. Kriegel, M. Schubert, and A. Zimek:
 * Angle-Based Outlier Detection in High-dimensional Data.
 * In: Proc. 14th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008.
 * 
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * 
 */
public class ABOD<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance> {
  /**
   * OptionID for {@link #FAST_FLAG}
   */
  public static final OptionID FAST_ID = OptionID.getOrCreateOptionID("abod.fast", 
      "Flag to indicate that the algorithm should run the fast/approximative version.");

  /**
   * Flag for fast mode
   */
  private final Flag FAST_FLAG = new Flag(FAST_ID);

  /**
   * Variable to store fast mode flag.
   */
  boolean fast = false;

  /**
   * OptionID for {@link #FAST_SAMPLE_PARAM}
   */
  public static final OptionID FAST_SAMPLE_ID = OptionID.getOrCreateOptionID("abod.samplesize", 
      "Sample size to use in fast mode.");

  /**
   * Parameter for sample size
   */
  private final IntParameter FAST_SAMPLE_PARAM = new IntParameter(FAST_SAMPLE_ID, new GreaterEqualConstraint(1), true);

  /**
   * Variable to store fast mode flag.
   */
  int sampleSize;

  /**
   * OptionID for {@link #KERNEL_FUNCTION_PARAM}
   */
  public static final OptionID KERNEL_FUNCTION_ID = OptionID.getOrCreateOptionID("abod.kernelfunction", 
      "Kernel function to use.");

  /**
   * Parameter for Kernel Function
   */
  // TODO: is a Polynomial Kernel the best default?
  private final ClassParameter<KernelFunction<V, DoubleDistance>> KERNEL_FUNCTION_PARAM =
    new ClassParameter<KernelFunction<V, DoubleDistance>>(KERNEL_FUNCTION_ID,
        KernelFunction.class, PolynomialKernelFunction.class.getCanonicalName());

  /**
   * Store the configured Kernel version
   */
  KernelFunction<V, DoubleDistance> kernelFunction;

  /**
   * Result storage.
   */
  ABODResult<V> result = null;

  /***************************************************************************
   * Constructor
   **************************************************************************/
  public ABOD() {
    addOption(FAST_FLAG);
    addOption(FAST_SAMPLE_PARAM);
    addOption(KERNEL_FUNCTION_PARAM);
  }

  public void getRanking(Database<V> data, int k) {
    KernelMatrix<V> kernelMatrix = new KernelMatrix<V>(kernelFunction, data);
    PriorityQueue<ComparablePair<Double, Integer>> pq = new PriorityQueue<ComparablePair<Double, Integer>>(data.size(), Collections.reverseOrder());

    for(Integer objKey : data.getIDs()) {
      MeanVariance s = new MeanVariance();

      // System.out.println("Processing: " +objKey);
      List<QueryResult<DoubleDistance>> neighbors = data.kNNQueryForID(objKey, k, getDistanceFunction());
      Iterator<QueryResult<DoubleDistance>> iter = neighbors.iterator();
      while(iter.hasNext()) {
        Integer key1 = iter.next().getID();
        // Iterator iter2 = data.keyIterator();
        Iterator<QueryResult<DoubleDistance>> iter2 = neighbors.iterator();
        // PriorityQueue best = new PriorityQueue(false, k);
        while(iter2.hasNext()) {
          Integer key2 = iter2.next().getID();
          if(key2.equals(key1) || key1.equals(objKey) || key2.equals(objKey))
            continue;
          double nenner = calcDenominator(kernelMatrix, objKey, key1, key2);

          if(nenner != 0) {
            double sqrtnenner = Math.sqrt(nenner);
            double tmp = calcNumerator(kernelMatrix, objKey, key1, key2) / nenner;
            s.addData(tmp, 1 / sqrtnenner);
          }

        }
      }
      pq.add(new ComparablePair<Double, Integer>(s.getVariance(), objKey));
    }
    ComparablePair<Double, Integer>[] reslist = ComparablePair.newArray(0);
    reslist = pq.toArray(reslist);
    Arrays.sort(reslist);
    result = new ABODResult<V>(data, reslist);
  }

  public void getFastRanking(Database<V> data, int k, int sampleSize) {
    KernelMatrix<V> kernelMatrix = new KernelMatrix<V>(kernelFunction, data);

    PriorityQueue<ComparablePair<Double, Integer>> pq = new PriorityQueue<ComparablePair<Double, Integer>>(data.size(), Collections.reverseOrder());
    // get Candidate Ranking
    for(Integer aKey : data.getIDs()) {
      HashMap<Integer, Double> dists = new HashMap<Integer, Double>(data.size());
      // determine kNearestNeighbors and pairwise distances
      PriorityQueue<ComparablePair<Double, Integer>> nn = calcDistsandNN(data, kernelMatrix, sampleSize, aKey, dists);
      if(false) {
        // alternative:
        PriorityQueue<ComparablePair<Double, Integer>> nn2 = calcDistsandRNDSample(data, kernelMatrix, sampleSize, aKey, dists);
      }

      // get normalization
      double[] counter = calcFastNormalization(aKey, dists);
      // System.out.println(counter[0] + " " + counter2[0] + " " + counter[1] +
      // " " + counter2[1]);
      // umsetzen von Pq zu list
      List<Integer> neighbors = new ArrayList<Integer>(nn.size());
      while(!nn.isEmpty())
        neighbors.add(nn.remove().getSecond());
      // getFilter
      double var = getAbofFilter(kernelMatrix, aKey, dists, counter[1], counter[0], neighbors);
      pq.add(new ComparablePair<Double, Integer>(var, aKey));
      // System.out.println("prog "+(prog++));
    }
    // refine Candidates
    PriorityQueue<ComparablePair<Double, Integer>> resqueue = new PriorityQueue<ComparablePair<Double, Integer>>(k);
    System.out.println(pq.size() + " objects ordered into candidate list.");
    int v = 0;
    while(!pq.isEmpty()) {
      if(resqueue.size() == k && pq.peek().getFirst() > resqueue.peek().getFirst())
        break;
      // double approx = pq.peek().getFirst();
      Integer aKey = pq.remove().getSecond();
      // if(!result.isEmpty())
      // System.out.println("Best Candidate " + aKey+" : " + pq.firstPriority()
      // + " worst result: " + result.firstPriority());
      // else
      // System.out.println("Best Candidate " + aKey+" : " + pq.firstPriority()
      // + " worst result: " + Double.MAX_VALUE);
      v++;
      MeanVariance s = new MeanVariance();
      for(Integer bKey : data.getIDs()) {
        if(bKey.equals(aKey))
          continue;
        for(Integer cKey : data.getIDs()) {
          if(cKey.equals(aKey))
            continue;
          // double nenner = dists[y]*dists[z];
          double nenner = calcDenominator(kernelMatrix, aKey, bKey, cKey);
          if(nenner != 0) {
            double tmp = calcNumerator(kernelMatrix, aKey, bKey, cKey) / nenner;
            double sqrtNenner = Math.sqrt(nenner);
            s.addData(tmp, 1 / sqrtNenner);
          }
        }
      }
      // System.out.println( aKey + "Sum " + sum + " SQRSum " +sqrSum +
      // " Counter " + counter);
      double var = s.getVariance();
      // System.out.println(aKey+ " : " + approx +" " + var);
      if(resqueue.size() < k) {
        resqueue.add(new ComparablePair<Double, Integer>(var, aKey));
      }
      else {
        if(resqueue.peek().getFirst() > var) {
          resqueue.remove();
          resqueue.add(new ComparablePair<Double, Integer>(var, aKey));
        }
      }

    }
    // System.out.println(v + " Punkte von " + data.size() + " verfeinert !!");
    ComparablePair<Double, Integer>[] reslist = ComparablePair.newArray(0);
    reslist = pq.toArray(reslist);
    Arrays.sort(reslist);
    this.result = new ABODResult<V>(data, reslist);
  }

  // TODO: remove?
  private double[] calcNormalization(Integer xKey, HashMap<Integer, Double> dists) {
    double[] result = new double[2];
    for(Integer yKey : dists.keySet()) {
      if(yKey.equals(xKey))
        continue;
      for(Integer zKey : dists.keySet()) {
        if(zKey <= yKey)
          continue;
        if(zKey.equals(xKey))
          continue;
        if(dists.get(yKey) != 0 && dists.get(zKey) != 0) {
          double sqr = Math.sqrt(dists.get(yKey) * dists.get(zKey));
          result[0] += 1 / sqr;
          result[1] += 1 / (dists.get(yKey) * dists.get(zKey) * sqr);
        }
      }
    }
    return result;
  }

  private double[] calcFastNormalization(Integer x, HashMap<Integer, Double> dists) {
    double[] result = new double[2];

    double sum = 0;
    double sumF = 0;
    for(Integer yKey : dists.keySet()) {
      if(dists.get(yKey) != 0) {
        double tmp = 1 / Math.sqrt(dists.get(yKey));
        sum += tmp;
        sumF += (1 / dists.get(yKey)) * tmp;
      }
    }
    double sofar = 0;
    double sofarF = 0;
    for(Integer zKey : dists.keySet()) {
      if(dists.get(zKey) != 0) {
        double tmp = 1 / Math.sqrt(dists.get(zKey));
        sofar += tmp;
        double rest = sum - sofar;
        result[0] += tmp * rest;

        sofarF += (1 / dists.get(zKey)) * tmp;
        double restF = sumF - sofarF;
        result[1] += (1 / dists.get(zKey)) * tmp * restF;
      }
    }
    return result;
  }

  // TODO: sum, sqrSum were always set to 0 on invocation.
  private double getAbofFilter(KernelMatrix<V> kernelMatrix, Integer aKey, HashMap<Integer, Double> dists, double fulCounter, double counter, List<Integer> neighbors) {
    MeanVariance s = new MeanVariance();
    double partCounter = 0;
    Iterator<Integer> iter = neighbors.iterator();
    while(iter.hasNext()) {
      Integer bKey = iter.next();
      if(bKey.equals(aKey))
        continue;
      Iterator<Integer> iter2 = neighbors.iterator();
      while(iter2.hasNext()) {
        Integer cKey = iter2.next();
        if(cKey.equals(aKey))
          continue;
        if(cKey > bKey) {
          double nenner = dists.get(bKey).doubleValue() * dists.get(cKey).doubleValue();
          if(nenner != 0) {
            double tmp = calcNumerator(kernelMatrix, aKey, bKey, cKey) / nenner;
            double sqrtNenner = Math.sqrt(nenner);
            s.addData(tmp, 1 / sqrtNenner);
            partCounter += (1 / (sqrtNenner * nenner));
          }
        }
      }
    }
    // TODO: Document the meaning / use of fulCounter, partCounter.
    double mu = (s.sum + (fulCounter - partCounter)) / counter;
    return (s.sqrSum / counter) - (mu * mu);
  }

  /**
   * Compute the cosinus value between vectors aKey and bKey.
   * 
   * @param kernelMatrix
   * @param aKey
   * @param bKey
   * @return
   */
  private double calcCos(KernelMatrix<V> kernelMatrix, Integer aKey, Integer bKey) {
    return kernelMatrix.getDistance(aKey, aKey) + kernelMatrix.getDistance(bKey, bKey) - 2 * kernelMatrix.getDistance(aKey, bKey);
  }

  private double calcDenominator(KernelMatrix<V> kernelMatrix, Integer aKey, Integer bKey, Integer cKey) {
    return calcCos(kernelMatrix, aKey, bKey) * calcCos(kernelMatrix, aKey, cKey);
  }

  private double calcNumerator(KernelMatrix<V> kernelMatrix, Integer aKey, Integer bKey, Integer cKey) {
    return (kernelMatrix.getDistance(aKey, aKey) + kernelMatrix.getDistance(bKey, cKey) - kernelMatrix.getDistance(aKey, cKey) - kernelMatrix.getDistance(aKey, bKey));
  }

  private PriorityQueue<ComparablePair<Double, Integer>> calcDistsandNN(Database<V> data, KernelMatrix<V> kernelMatrix, int sampleSize, Integer aKey, HashMap<Integer, Double> dists) {
    PriorityQueue<ComparablePair<Double, Integer>> nn = new PriorityQueue<ComparablePair<Double, Integer>>(sampleSize);
    for(Integer bKey : data.getIDs()) {
      double val = calcCos(kernelMatrix, aKey, bKey);
      dists.put(bKey, val);
      if(nn.size() < sampleSize) {
        nn.add(new ComparablePair<Double, Integer>(val, bKey));
      }
      else {
        if(val < nn.peek().getFirst()) {
          nn.remove();
          nn.add(new ComparablePair<Double, Integer>(val, bKey));
        }
      }
    }
    return nn;
  }

  private PriorityQueue<ComparablePair<Double, Integer>> calcDistsandRNDSample(Database<V> data, KernelMatrix<V> kernelMatrix, int sampleSize, Integer aKey, HashMap<Integer, Double> dists) {
    PriorityQueue<ComparablePair<Double, Integer>> nn = new PriorityQueue<ComparablePair<Double, Integer>>(sampleSize);
    int step = (int) ((double) data.size() / (double) sampleSize);
    int counter = 0;
    for(Integer bKey : data.getIDs()) {
      double val = calcCos(kernelMatrix, aKey, bKey);
      dists.put(bKey, val);
      if(counter % step == 0)
        nn.add(new ComparablePair<Double, Integer>(val, bKey));
      counter++;
    }
    return nn;
  }

  // TODO: this should be done by the result classes.
  public void getExplanations(Database<V> data) {
    KernelMatrix<V> kernelMatrix = new KernelMatrix<V>(kernelFunction, data);
    // PQ for Outlier Ranking
    PriorityQueue<ComparablePair<Double, Integer>> pq = new PriorityQueue<ComparablePair<Double, Integer>>(data.size(), Collections.reverseOrder());
    HashMap<Integer, LinkedList<Integer>> explaintab = new HashMap<Integer, LinkedList<Integer>>();
    // test all objects
    for(Integer objKey : data.getIDs()) {
      MeanVariance s = new MeanVariance();
      // Queue for the best explanation
      PriorityQueue<ComparablePair<Double, Integer>> explain = new PriorityQueue<ComparablePair<Double, Integer>>();
      // determine Object
      // for each pair of other objects
      Iterator<Integer> iter = data.iterator();
      // Collect Explanation Vectors
      while(iter.hasNext()) {
        MeanVariance s2 = new MeanVariance();
        Integer key1 = iter.next();
        Iterator<Integer> iter2 = data.iterator();
        if(objKey.equals(key1))
          continue;
        while(iter2.hasNext()) {
          Integer key2 = iter2.next();
          if(key2.equals(key1) || objKey.equals(key2))
            continue;
          double nenner = calcDenominator(kernelMatrix, objKey, key1, key2);
          if(nenner != 0) {
            double tmp = calcNumerator(kernelMatrix, objKey, key1, key2) / nenner;
            double sqr = Math.sqrt(nenner);
            s2.addData(tmp, 1 / sqr);
          }
        }
        explain.add(new ComparablePair<Double, Integer>(s2.getVariance(), key1));
        s.addData(s2);
      }
      // build variance of the observed vectors
      pq.add(new ComparablePair<Double, Integer>(s.getVariance(), objKey));
      //
      LinkedList<Integer> expList = new LinkedList<Integer>();
      expList.add(explain.remove().getSecond());
      while(!explain.isEmpty()) {
        Integer nextKey = explain.remove().getSecond();
        if(nextKey.equals(objKey))
          continue;
        double max = Double.MIN_VALUE;
        for(Integer exp : expList) {
          if(exp.equals(objKey) || nextKey.equals(exp))
            continue;
          double nenner = Math.sqrt(calcCos(kernelMatrix, objKey, nextKey)) * Math.sqrt(calcCos(kernelMatrix, objKey, exp));
          double angle = calcNumerator(kernelMatrix, objKey, nextKey, exp) / nenner;
          max = Math.max(angle, max);
        }
        if(max < 0.5)
          expList.add(nextKey);
      }
      explaintab.put(objKey, expList);
    }
    System.out.println("--------------------------------------------");
    System.out.println("Result: ABOD");
    int count = 0;
    while(!pq.isEmpty()) {
      if(count > 10)
        break;
      double factor = pq.peek().getFirst();
      Integer key = pq.remove().getSecond();
      System.out.print(data.get(key) + " ");
      System.out.println(count + " Factor=" + factor + " " + key);
      LinkedList<Integer> expList = explaintab.get(key);
      generateExplanation(data, key, expList);
      count++;
    }
    System.out.println("--------------------------------------------");
  }

  private void generateExplanation(Database<V> data, Integer key, LinkedList<Integer> expList) {
    V vect1 = data.get(key);
    Iterator<Integer> iter = expList.iterator();
    while(iter.hasNext()) {
      System.out.println("Outlier: " + vect1);
      V exp = data.get(iter.next());
      System.out.println("Most common neighbor: " + exp);
      // determine difference Vector
      V vals = exp.plus(vect1.negativeVector());
      System.out.println(vals);
      // System.out.println(new FeatureVector(
      // "Diff-"+vect1.getPrimaryKey(),vals ));
    }
    System.out.println();
  }

  @Override
  protected void runInTime(Database<V> database) throws IllegalStateException {
    this.kernelFunction.setDatabase(database, false, false);
    if(fast) {
      getFastRanking(database, 30, sampleSize);
    }
    else {
      getRanking(database, 30);
    }
  }

  /**
   * Return a description of the algorithm.
   */
  @Override
  public Description getDescription() {
    return new Description("ABOD", "Angle-Based Outlier Detection",
        "Outlier detection using variance analysis on angles, especially for high dimensional data sets.",
        "H.-P. Kriegel, M. Schubert, and A. Zimek: "
        + "Angle-Based Outlier Detection in High-dimensional Data. "
        + "In: Proc. 14th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008.");
  }

  /**
   * Return the results of the last run.
   */
  @Override
  public Result<V> getResult() {
    return result;
  }

  /**
   * Calls the super method and adds to the returned attribute settings the
   * attribute settings of the {@link #dependencyDerivator}.
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(kernelFunction.getAttributeSettings());
    return attributeSettings;
  }

  /**
   * Calls the super method and sets parameters {@link FAST_FLAG},
   * {@link FAST_SAMPLE_PARAM} and {@link KERNEL_FUNCTION_PARAM}. The remaining
   * parameters are then passed to the {@link #kernelFunction}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    fast = FAST_FLAG.getValue();

    if(fast)
      sampleSize = FAST_SAMPLE_PARAM.getValue();

    kernelFunction = KERNEL_FUNCTION_PARAM.instantiateClass();
    remainingParameters = kernelFunction.setParameters(remainingParameters);

    return remainingParameters;
  }
}