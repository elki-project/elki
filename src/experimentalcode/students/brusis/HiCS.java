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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
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
 * 
 * Reference:
 * <p>
 * Fabian Keller, Emmanuel Müller, Klemens Böhm:<br />
 * HiCS: High Contrast Subspaces for Density-Based Outlier Ranking<br />
 * in: Proc. IEEE 28th Int. Conf. on Data Engineering (ICDE 2012), Washington,
 * DC, USA
 * </p>
 * 
 * @author Jan Brusis
 * 
 * @param <V> vector type
 */
@Title("HiCS: High Contrast Subspaces for Density-Based Outlier Ranking")
@Description("Algorithm to compute High Contrast Subspaces in a database as a pre-processing step for for density-based outlier ranking methods.")
@Reference(authors = "Fabian Keller, Emmanuel Müller, Klemens Böhm", title = "HiCS: High Contrast Subspaces for Density-Based Outlier Ranking", booktitle = "Proc. IEEE 28th International Conference on Data Engineering (ICDE 2012)")
public class HiCS<V extends NumberVector<?, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The Logger for this class
   */
  private static final Logging logger = Logging.getLogger(HiCS.class);

  /**
   * Holds the value of {@link #M_ID}.
   */
  private int m;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  // /**
  // * Holds the value of {@link #ALGO_ID}
  // */
  // private OutlierAlgorithm outlierAlgorithm;

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
  private ArrayList<ArrayDBIDs> subspaceIndex;

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
    // this.outlierAlgorithm = outlierAlgorithm;
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
  public OutlierResult run(Relation<V> relation) {
    this.subspaceIndex = calculateIndices(relation);

    Set<BitSet> subspaces = calculateSubspaces(relation);

    System.out.println("Number of high-contrast subspaces: " + subspaces.size());
    List<OutlierResult> results = new ArrayList<OutlierResult>();
    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Calculating Outlier scores for high Contrast subspaces", subspaces.size(), logger) : null;

    // run outlier detection and collect the result
    // TODO extend so that any outlierAlgorithm can be used (use materialized
    // relation instead of SubspaceEuclideanDistanceFunction?)
    LOF<V, DoubleDistance> lof;
    for(BitSet dimset : subspaces) {
      SubspaceEuclideanDistanceFunction df = new SubspaceEuclideanDistanceFunction(dimset);
      lof = new LOF<V, DoubleDistance>(100, df, df);

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
   * stores them in a list
   * 
   * @param relation Relation to index
   * @return List of sorted objects
   */
  private ArrayList<ArrayDBIDs> calculateIndices(Relation<? extends NumberVector<?, ?>> relation) {
    final int dim = DatabaseUtil.dimensionality(relation);
    ArrayList<ArrayDBIDs> subspaceIndex = new ArrayList<ArrayDBIDs>(dim + 1);
    subspaceIndex.add(null); // dimension 0 does not exist

    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    for(int i = 1; i <= dim; i++) {
      ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(relation.getDBIDs());
      comp.setDimension(i);
      amDBIDs.sort(comp);
      subspaceIndex.add(amDBIDs);
    }

    return subspaceIndex;
  }

  /**
   * Identifies high contrast subspaces in a given full-dimensional database
   * 
   * @param relation the relation the HiCS should be evaluated for
   * @return a set of high contrast subspaces
   */
  private Set<BitSet> calculateSubspaces(Relation<? extends NumberVector<?, ?>> relation) {
    final int dbdim = DatabaseUtil.dimensionality(relation);

    Set<BitSet> subspaces = new HashSet<BitSet>();

    TreeSet<HiCSSubspace> subspaceList = new TreeSet<HiCSSubspace>(HiCSSubspace.SORT_BY_CONTRAST);
    TreeSet<HiCSSubspace> dDimensionalList = new TreeSet<HiCSSubspace>(HiCSSubspace.SORT_BY_CONTRAST);
    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Generating two-element subsets", dbdim * (dbdim - 1) / 2, logger) : null;
    // compute two-element sets of subspaces
    for(int i = 1; i < dbdim; i++) {
      for(int j = i + 1; j <= dbdim; j++) {
        HiCSSubspace ts = new HiCSSubspace();
        ts.set(i);
        ts.set(j);
        calculateHiCS(relation, ts);
        if(dDimensionalList.size() < cutoff) {
          dDimensionalList.add(ts);
        }
        else if(ts.contrast > dDimensionalList.last().contrast) {
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
      subspaceList.addAll(dDimensionalList);
      // result now contains all d-dimensional sets of subspaces

      ArrayList<HiCSSubspace> candidateList = new ArrayList<HiCSSubspace>(cutoff);
      Iterator<HiCSSubspace> iter = dDimensionalList.iterator();

      for(int i = 0; i < cutoff; i++) {
        if(iter.hasNext()) {
          HiCSSubspace s = iter.next();
          candidateList.add(s);
        }
        else {
          break;
        }
      }
      dDimensionalList.clear();
      // candidateList now contains the *m* best d-dimensional sets
      Collections.sort(candidateList, HiCSSubspace.SORT_BY_SUBSPACE);

      for(int i = 0; i < candidateList.size() - 1; i++) {
        for(int j = i + 1; j < candidateList.size(); j++) {
          HiCSSubspace set1 = candidateList.get(i);
          HiCSSubspace set2 = candidateList.get(j);

          if(HiCSSubspace.SORT_BY_SUBSPACE.compare(set1, set2) == 0) {
            logger.warning("No overlap?!?");
            continue;
          }

          HiCSSubspace joinedSet = new HiCSSubspace();
          joinedSet.or(set1);
          joinedSet.or(set2);
          calculateHiCS(relation, joinedSet);

          if(dDimensionalList.size() < cutoff) {
            dDimensionalList.add(joinedSet);
          }
          else if(joinedSet.contrast > dDimensionalList.last().contrast) {
            dDimensionalList.pollLast();
            dDimensionalList.add(joinedSet);
          }
        }
      }
      // Prune
      System.out.println("List before pruning: " + subspaceList.size());
      for(int i = 0; i < candidateList.size(); i++) {
        for(HiCSSubspace nextSet : dDimensionalList) {
          if(nextSet.contrast > candidateList.get(i).contrast) {
            subspaceList.remove(candidateList.get(i));
            break;
          }
        }
      }
      System.out.println("List after pruning: " + subspaceList.size());
      d++;
    }

    Iterator<HiCSSubspace> it = subspaceList.iterator();

    for(int i = 0; i < subspaceList.size(); i++) {
      HiCSSubspace set = it.next();
      subspaces.add(set);
    }
    return subspaces;
  }

  /**
   * Calculates the actual contrast of a given subspace
   * 
   * @param relation
   * @param subspace
   */
  private void calculateHiCS(Relation<? extends NumberVector<?, ?>> relation, HiCSSubspace subspace) {
    final int card = subspace.cardinality();
    final double alpha1 = Math.pow(alpha, (1.0 / card));
    final int windowsize = (int) (relation.size() * alpha1);
    final Random random = new Random();
    final FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Monte-Carlo iterations", m, logger) : null;

    int retries = 0;
    double deviationSum = 0.0;
    for(int i = 0; i < m; i++) {
      // Choose a random set bit.
      int chosen = -1;
      for(int tmp = random.nextInt(card); tmp >= 0; tmp--) {
        chosen = subspace.nextSetBit(chosen + 1);
      }
      // initialize sample
      DBIDs conditionalSample = subspaceIndex.get(1);

      for(int j = subspace.nextSetBit(0); j >= 0; j = subspace.nextSetBit(j + 1)) {
        if(j == chosen) {
          continue;
        }
        ArrayDBIDs sortedIndices = subspaceIndex.get(j);
        ArrayModifiableDBIDs indexBlock = DBIDUtil.newArray();
        // initialize index block
        int start = random.nextInt(relation.size() - windowsize);
        for(int k = start; k < start + windowsize; k++) {
          indexBlock.add(sortedIndices.get(k)); // select index block
        }

        conditionalSample = DBIDUtil.intersection(conditionalSample, indexBlock);
      }
      if(conditionalSample.size() < 10) {
        i--;
        retries++;
        logger.warning("Sample size very small. Retry no. " + retries);
        continue;
      }
      // Conditional set
      double[] sampleValues = new double[conditionalSample.size()];
      {
        int l = 0;
        for(DBID id : conditionalSample) {
          sampleValues[l] = relation.get(id).doubleValue(chosen);
          l++;
        }
      }
      // Full set
      double[] fullValues = new double[relation.size()];
      {
        int l = 0;
        for(DBID id : subspaceIndex.get(chosen)) {
          fullValues[l] = relation.get(id).doubleValue(chosen);
          l++;
        }
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if(Double.isNaN(contrast)) {
        i--;
        logger.warning("Contrast was NaN");
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
    subspace.contrast = deviationSum / m;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * BitSet that holds a contrast value as field. Used for the representation of
   * a subspace in HiCS
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class HiCSSubspace extends BitSet {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * The HiCS contrast value
     */
    protected double contrast;

    /**
     * Constructor.
     */
    public HiCSSubspace() {
      super();
    }

    /**
     * Sort subspaces by their actual subspace.
     */
    public static Comparator<HiCSSubspace> SORT_BY_CONTRAST = new Comparator<HiCSSubspace>() {
      @Override
      public int compare(HiCSSubspace o1, HiCSSubspace o2) {
        if(o1.contrast == o2.contrast) {
          return 0;
        }
        return o1.contrast < o2.contrast ? 1 : -1;
      }
    };

    /**
     * Sort subspaces by their actual subspace.
     */
    public static Comparator<HiCSSubspace> SORT_BY_SUBSPACE = new Comparator<HiCSSubspace>() {
      @Override
      public int compare(HiCSSubspace o1, HiCSSubspace o2) {
        int dim1 = o1.nextSetBit(0);
        int dim2 = o2.nextSetBit(0);
        while(dim1 >= 0 && dim2 >= 0) {
          if(dim1 < dim2) {
            return -1;
          }
          else if(dim1 > dim2) {
            return 1;
          }
          dim1 = o1.nextSetBit(dim1 + 1);
          dim2 = o2.nextSetBit(dim2 + 1);
        }
        return 0;
      }
    };
  }

  /**
   * Parameterization class
   * 
   * @author Jan Brusis
   * 
   * @apiviz.exclude
   * 
   * @param <V> vector type
   */
  public static class Parameterizer<V extends NumberVector<?, ?>> extends AbstractParameterizer {
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
    protected HiCS<V> makeInstance() {
      return new HiCS<V>(m, alpha, outlierAlgorithm, statTest, cutoff);
    }
  }
}