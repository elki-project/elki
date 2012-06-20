package experimentalcode.students.brusis;

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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;

import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Algorithm to compute High Contrast Subspaces for Density-Based Outlier
 * Ranking.
 * <p/>
 * 
 * @author Jan Brusis
 */
@Title("HiCS: High Contrast Subspaces for Density-Based Outlier Ranking")
@Description("Algorithm to compute High Contrast Subspaces in a database as a pre-processing step for for density-based outlier ranking methods.")
public class HiCS extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {

  /**
   * The Logger for this class
   */
  private static final Logging logger = Logging.getLogger(HiCS.class);

  /**
   * Parameter that specifies the number of iterations in the Monte-Carlo
   * process of identifying high contrast subspaces
   */
  public static final OptionID M_ID = OptionID.getOrCreateOptionID("hics.m", "The number of iterations in the Monte-Carlo processing.");

  /**
   * Parameter that determines the size of the test statistic during the
   * Monte-Carlo iteration
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hics.alpha", "The discriminance value that determines the size of the test statistic .");

  /**
   * Parameter that specifies which outlier detection algorithm to use on the
   * resulting set of high contrast subspaces
   */
  public static final OptionID ALGO_ID = OptionID.getOrCreateOptionID("hics.algo", "The Algorithm that performs the actual outlier detection on the resulting set of subspace");

  /**
   * Parameter that specifies which statistical test to use in order to
   * calculate the deviation of two given data samples
   */
  public static final OptionID TEST_ID = OptionID.getOrCreateOptionID("hics.test", "The statistical test that is used to calculate the deviation of two data samples");

  /**
   * Parameter that specifies the candidate_cutoff
   */
  public static final OptionID LIMIT_ID = OptionID.getOrCreateOptionID("hics.limit", "The threshold that determines how many d-dimensional subspace candidates to retain in each step of the generation");

  /**
   * Holds the value of {@link #M_ID}.
   */
  private int m;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

//  /**
//   * Holds the value of {@link #ALGO_ID}
//   */
//  private OutlierAlgorithm outlierAlgorithm;

  /**
   * Holds the value of{@link #TEST_ID}
   */
  private GoodnessOfFitTest statTest;

  /**
   * Holds the value of {@link #LIMIT_ID}
   */
  private int cutoff;

  /**
   * Holds sorted indices for every attribute of the relation
   */
  private Map<Integer, ArrayModifiableDBIDs> subspaceIndex;

  /**
   * Constructor
   * 
   * @param m value of m
   * @param alpha value of alpha
   */
  public HiCS(int m, double alpha, OutlierAlgorithm outlierAlgorithm, GoodnessOfFitTest statTest, int cutoff) {
    super();
    this.m = m;
    this.alpha = alpha;
//    this.outlierAlgorithm = outlierAlgorithm;
    this.statTest = statTest;
    this.cutoff = cutoff;
  }

  /**
   * Perform HiCS on a given database
   * 
   * @param relation the database
   * @return The aggregated resulting scores that were assigned by the given
   *         outlier detection algorithm
   */
  public OutlierResult run(Relation<NumberVector<?, ?>> relation) {

    this.subspaceIndex = calculateIndices(relation);

    Set<BitSet> subspaces = calculateSubspaces(relation);
    
    System.out.println("Number of high-contrast subspaces: " + subspaces.size());
    List<OutlierResult> results = new ArrayList<OutlierResult>();
    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Calculating Outlier scores for high Contrast subspaces", subspaces.size(), logger) : null;

    // run outlier detection and collect the result
    //TODO extend so that any outlierAlgorithm can be used (use materialized relation instead of SubspaceEuclideanDistanceFunction?)
    LOF<NumberVector<?, ?>, DoubleDistance> lof;
    for(BitSet dimset : subspaces) {
      SubspaceEuclideanDistanceFunction df = new SubspaceEuclideanDistanceFunction(dimset);
      lof = new LOF<NumberVector<?, ?>, DoubleDistance>(100, df, df);

      // run LOF and collect the result
      OutlierResult result = lof.run(relation);
      results.add(result);
      if(prog != null) {
        prog.incrementProcessed(logger);
      }
    }
    subspaces = null;
    if(prog != null) {
      prog.ensureCompleted(logger);
    }

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBID id : relation.iterDBIDs()) {
      double sum = 0.0;
      for(OutlierResult r : results) {
        final Double s = r.getScores().get(id);
        if(s != null && !Double.isNaN(s)) {
          sum += s;
        }
      }
      scores.putDouble(id, sum);
      minmax.put(sum);
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    Relation<Double> scoreres = new MaterializedRelation<Double>("HiCS", "HiCS-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());

    return new OutlierResult(meta, scoreres);
  }

  /**
   * Calculates "index structures" for every attribute, i.e. sorts a
   * ModifiableArray of every DBID in the database for every dimension and
   * stores them in a hash
   * 
   * @param relation
   * @return
   */
  private Map<Integer, ArrayModifiableDBIDs> calculateIndices(Relation<NumberVector<?, ?>> relation) {
    Map<Integer, ArrayModifiableDBIDs> subspaceIndex = new HashMap<Integer, ArrayModifiableDBIDs>();
    ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(relation.size());
    amDBIDs.addDBIDs(relation.getDBIDs());
    SubspaceIndexComparator comparator = new SubspaceIndexComparator(0, relation);

    for(int i = 1; i <= DatabaseUtil.dimensionality(relation); i++) {
      comparator.changeDimension(i);
      amDBIDs.sort(comparator);
      subspaceIndex.put(i, DBIDUtil.newArray(amDBIDs));
    }

    return subspaceIndex;
  }

  /**
   * Identifies high contrast subspaces in a given full-dimensional database
   * 
   * @param relation the relation the HiCS should be evaluated for
   * @return a set of high contrast subspaces
   */
  private Set<BitSet> calculateSubspaces(Relation<NumberVector<?, ?>> relation) {
    final int dbdim = DatabaseUtil.dimensionality(relation);

    Set<BitSet> subspaces = new HashSet<BitSet>();

    TreeSet<SubspaceSet<Integer>> subspaceList = new TreeSet<SubspaceSet<Integer>>();
    TreeSet<SubspaceSet<Integer>> dDimensionalList = new TreeSet<SubspaceSet<Integer>>();
    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Generating two-element subsets", dbdim * (dbdim - 1) / 2, logger) : null;
    // compute two-element sets of subspaces
    for(int i = 1; i < dbdim; i++) {
      for(int j = i + 1; j <= dbdim; j++) {
        SubspaceSet<Integer> ts = new SubspaceSet<Integer>();
        ts.add(i);
        ts.add(j);
        double contrast = calculateHiCS(relation, new ArrayList<Integer>(ts));
        ts.setContrast(contrast);
        if(dDimensionalList.size() < cutoff) {
          dDimensionalList.add(ts);
        }
        else if(contrast > dDimensionalList.last().getContrast()) {
          dDimensionalList.pollLast();
          dDimensionalList.add(ts);
        }
        if(prog != null) {
          prog.incrementProcessed(logger);
        }
      }
    }
    if(prog != null) {
      prog.ensureCompleted(logger);
    }

    int d = 2;
    while(!dDimensionalList.isEmpty()) {
      System.out.println("Dimension: " + d);
      subspaceList.addAll(dDimensionalList); // result now contains all
                                             // d-dimensional sets of subspaces

      ArrayList<SubspaceSet<Integer>> candidateList = new ArrayList<SubspaceSet<Integer>>(cutoff);
      Iterator<SubspaceSet<Integer>> iter = dDimensionalList.iterator();

      for(int i = 0; i < cutoff; i++) {
        if(iter.hasNext()) {
          SubspaceSet<Integer> s = iter.next();
          candidateList.add(s);
        }
        else {
          break;
        }
      }
      dDimensionalList.clear(); // candidateList now contains the *m* best
                                // d-dimensional sets
      Collections.sort(candidateList, new Comparator<SubspaceSet<Integer>>() {

        @Override
        public int compare(SubspaceSet<Integer> arg1, SubspaceSet<Integer> arg2) {
          Iterator<Integer> iter1 = arg1.iterator();
          Iterator<Integer> iter2 = arg2.iterator();
          for(int k = 0; k < arg1.size(); k++) {
            int i = iter1.next();
            int j = iter2.next();
            if(i < j) {
              return -1;
            }
            else if(i > j) {
              return 1;
            }
          }
          return 0;
        }
      });

      for(int i = 0; i < candidateList.size() - 1; i++) {
        for(int j = i + 1; j < candidateList.size(); j++) {
          SubspaceSet<Integer> joinedSet = null;
          SubspaceSet<Integer> set1 = candidateList.get(i);
          SubspaceSet<Integer> set2 = candidateList.get(j);
          Iterator<Integer> iter1 = set1.iterator();
          Iterator<Integer> iter2 = set2.iterator();
          boolean join = true;
          for(int k = 1; k < d; k++) {
            if(iter1.next() != iter2.next()) {
              join = false;
              break;
            }
          }
          if(!join) {
            continue;
          }
          else if(iter1.next() != iter2.next()) {
            joinedSet = new SubspaceSet<Integer>();
            joinedSet.addAll(set1);
            joinedSet.addAll(set2);
            double contrast = calculateHiCS(relation, new ArrayList<Integer>(joinedSet));
            joinedSet.setContrast(contrast);
          }

          if(joinedSet == null) {
            System.err.println("This shouldn't have happened :/");
            continue;
          }

          if(dDimensionalList.size() < cutoff) {
            dDimensionalList.add(joinedSet);
          }
          else if(joinedSet.getContrast() > dDimensionalList.last().getContrast()) {
            dDimensionalList.pollLast();
            dDimensionalList.add(joinedSet);
          }
        }
      }
      // Prune
      System.out.println("List before pruning: " + subspaceList.size());
      for(int i = 0; i < candidateList.size(); i++) {
        for(SubspaceSet<Integer> nextSet : dDimensionalList) {
          if(nextSet.getContrast() > candidateList.get(i).getContrast()) {
            subspaceList.remove(candidateList.get(i));
            break;
          }
        }
      }
      System.out.println("List after pruning: " + subspaceList.size());
      d++;
    }

    Iterator<SubspaceSet<Integer>> it = subspaceList.iterator();

    for(int i = 0; i < subspaceList.size(); i++) {
      SubspaceSet<Integer> set = it.next();
      System.out.println(set + " Contrast: " + set.getContrast());
      subspaces.add(set.BitSetRepresentation());
    }
    return subspaces;
  }

  /**
   * Calculates the actual contrast of a given subspace
   * 
   * @param relation
   * @param subspace
   * @return
   */
  private double calculateHiCS(Relation<NumberVector<?, ?>> relation, ArrayList<Integer> subspace) {
    double deviationSum = 0.0;
    double alpha1 = Math.pow(alpha, (1.0 / subspace.size()));
    int error = 1;
    int size = (int) (relation.size() * alpha1);
    Random random = new Random();
    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Iteration", m, logger) : null;
    for(int i = 0; i < m; i++) {
      double[] fullValues;
      double[] sampleValues;
      ArrayModifiableDBIDs sample;

      Collections.shuffle(subspace);
      ModifiableDBIDs conditionalSample = subspaceIndex.get(1); // initialize
                                                                // sample

      for(int j = 0; j < subspace.size() - 1; j++) {
        int start = random.nextInt(relation.size() - size);
        ArrayModifiableDBIDs sortedIndices = subspaceIndex.get(subspace.get(j));
        ArrayModifiableDBIDs indexBlock = DBIDUtil.newArray();
        // initialize index block
        for(int k = start; k < start + size; k++) {
          indexBlock.add(sortedIndices.get(k)); // select index block
        }

        conditionalSample = DBIDUtil.intersection(conditionalSample, indexBlock);
      }
      if(conditionalSample.size() < 10) {
        i--;
        error++;
        System.err.println("Warning: Sample size very small. Retry no. " + error);
        continue;
      }

      int remainingAttribute = subspace.get(subspace.size() - 1);
      sample = DBIDUtil.newArray(conditionalSample);
      fullValues = new double[relation.size()];
      sampleValues = new double[conditionalSample.size()];
      for(int l = 0; l < relation.size(); l++) {
        fullValues[l] = relation.get(subspaceIndex.get(remainingAttribute).get(l)).doubleValue(remainingAttribute);
      }
      for(int l = 0; l < conditionalSample.size(); l++) {
        sampleValues[l] = relation.get(sample.get(l)).doubleValue(remainingAttribute);
      }
      conditionalSample = null;
      sample = null;
      double contrast = statTest.deviation(fullValues, sampleValues);

      if(Double.isNaN(contrast)) {
        i--;
        System.err.println("Contrast was NaN");
        continue;
      }
      deviationSum += contrast;
      if(prog != null) {
        prog.incrementProcessed(logger);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(logger);
    }
    return deviationSum / m;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  public static class Parameterizer extends AbstractParameterizer {

    /**
     * Holds the value of {@link #M_ID}.
     */
    private int m = 50;

    /**
     * Holds the value of {@link #ALPHA_ID}.
     */
    private double alpha = 0.1;

    /**
     * Holds the value of {@link #ALGO_ID}.
     */
    private OutlierAlgorithm outlierAlgorithm;

    /**
     * Holds the value of {@link #TEST_ID}.
     */
    private GoodnessOfFitTest statTest;

    /**
     * Holds the value of {@link #LIMIT_ID}
     */
    private int cutoff = 400;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter mP = new IntParameter(M_ID, new GreaterConstraint(1), 50);
      if(config.grab(mP)) {
        m = mP.getValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 0.1);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }

      final ClassParameter<OutlierAlgorithm> algoP = new ClassParameter<OutlierAlgorithm>(ALGO_ID, OutlierAlgorithm.class, LOF.class);
      if(config.grab(algoP)) {
        outlierAlgorithm = algoP.instantiateClass(config);
      }

      final ClassParameter<GoodnessOfFitTest> testP = new ClassParameter<GoodnessOfFitTest>(TEST_ID, GoodnessOfFitTest.class, KolmogorovSmirnovTest.class);
      if(config.grab(testP)) {
        statTest = testP.instantiateClass(config);
      }

      final IntParameter cutoffP = new IntParameter(LIMIT_ID, new GreaterConstraint(1), 400);
      if(config.grab(cutoffP)) {
        cutoff = cutoffP.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new HiCS(m, alpha, outlierAlgorithm, statTest, cutoff);
    }

  }

}
