package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

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
import de.lmu.ifi.dbs.elki.data.projection.NumericalFeatureSelection;
import de.lmu.ifi.dbs.elki.data.projection.Projection;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ProjectedView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

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
 * @author Erich Schubert
 * 
 * @apiviz.composedOf GoodnessOfFitTest
 * @apiviz.composedOf OutlierAlgorithm
 * 
 * @param <V> vector type
 */
@Title("HiCS: High Contrast Subspaces for Density-Based Outlier Ranking")
@Description("Algorithm to compute High Contrast Subspaces in a database as a pre-processing step for for density-based outlier ranking methods.")
@Reference(authors = "Fabian Keller, Emmanuel Müller, Klemens Böhm", title = "HiCS: High Contrast Subspaces for Density-Based Outlier Ranking", booktitle = "Proc. IEEE 28th International Conference on Data Engineering (ICDE 2012)", url="http://dx.doi.org/10.1109/ICDE.2012.88")
public class HiCS<V extends NumberVector<?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The Logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HiCS.class);

  /**
   * Maximum number of retries.
   */
  private static final int MAX_RETRIES = 100;

  /**
   * Monte-Carlo iterations.
   */
  private int m;

  /**
   * Alpha threshold.
   */
  private double alpha;

  /**
   * Outlier detection algorithm.
   */
  private OutlierAlgorithm outlierAlgorithm;

  /**
   * Statistical test to use.
   */
  private GoodnessOfFitTest statTest;

  /**
   * Candidates limit.
   */
  private int cutoff;
  
  /**
   * Random generator.
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param m value of m
   * @param alpha value of alpha
   * @param outlierAlgorithm Inner outlier detection algorithm
   * @param statTest Test to use
   * @param cutoff Candidate limit
   * @param rnd Random generator
   */
  public HiCS(int m, double alpha, OutlierAlgorithm outlierAlgorithm, GoodnessOfFitTest statTest, int cutoff, RandomFactory rnd) {
    super();
    this.m = m;
    this.alpha = alpha;
    this.outlierAlgorithm = outlierAlgorithm;
    this.statTest = statTest;
    this.cutoff = cutoff;
    this.rnd = rnd;
  }

  /**
   * Perform HiCS on a given database.
   * 
   * @param relation the database
   * @return The aggregated resulting scores that were assigned by the given
   *         outlier detection algorithm
   */
  public OutlierResult run(Relation<V> relation) {
    final DBIDs ids = relation.getDBIDs();
    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(relation);

    ArrayList<ArrayDBIDs> subspaceIndex = buildOneDimIndexes(relation);
    Set<HiCSSubspace> subspaces = calculateSubspaces(relation, subspaceIndex, rnd.getRandom());

    if(LOG.isVerbose()) {
      LOG.verbose("Number of high-contrast subspaces: " + subspaces.size());
    }
    List<Relation<Double>> results = new ArrayList<Relation<Double>>();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Calculating Outlier scores for high Contrast subspaces", subspaces.size(), LOG) : null;

    // run outlier detection and collect the result
    // TODO extend so that any outlierAlgorithm can be used (use materialized
    // relation instead of SubspaceEuclideanDistanceFunction?)
    for(HiCSSubspace dimset : subspaces) {
      if(LOG.isVerbose()) {
        LOG.verbose("Performing outlier detection in subspace " + dimset);
      }

      ProxyDatabase pdb = new ProxyDatabase(ids);
      Projection<V, V> proj = new NumericalFeatureSelection<V>(dimset, factory);
      pdb.addRelation(new ProjectedView<V, V>(relation, proj));

      // run LOF and collect the result
      OutlierResult result = outlierAlgorithm.run(pdb);
      results.add(result.getScores());
      if(prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(LOG);
    }

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double sum = 0.0;
      for(Relation<Double> r : results) {
        final Double s = r.get(iditer);
        if(s != null && !Double.isNaN(s)) {
          sum += s;
        }
      }
      scores.putDouble(iditer, sum);
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
  private ArrayList<ArrayDBIDs> buildOneDimIndexes(Relation<? extends NumberVector<?>> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    ArrayList<ArrayDBIDs> subspaceIndex = new ArrayList<ArrayDBIDs>(dim + 1);

    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    for(int i = 0; i < dim; i++) {
      ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(relation.getDBIDs());
      comp.setDimension(i);
      amDBIDs.sort(comp);
      subspaceIndex.add(amDBIDs);
    }

    return subspaceIndex;
  }

  /**
   * Identifies high contrast subspaces in a given full-dimensional database.
   * 
   * @param relation the relation the HiCS should be evaluated for
   * @param subspaceIndex Subspace indexes
   * @return a set of high contrast subspaces
   */
  private Set<HiCSSubspace> calculateSubspaces(Relation<? extends NumberVector<?>> relation, ArrayList<ArrayDBIDs> subspaceIndex, Random random) {
    final int dbdim = RelationUtil.dimensionality(relation);

    FiniteProgress dprog = LOG.isVerbose() ? new FiniteProgress("Subspace dimensionality", dbdim, LOG) : null;
    if(dprog != null) {
      dprog.setProcessed(2, LOG);
    }

    TreeSet<HiCSSubspace> subspaceList = new TreeSet<HiCSSubspace>(HiCSSubspace.SORT_BY_SUBSPACE);
    TopBoundedHeap<HiCSSubspace> dDimensionalList = new TopBoundedHeap<HiCSSubspace>(cutoff, HiCSSubspace.SORT_BY_CONTRAST_ASC);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Generating two-element subsets", (dbdim * (dbdim - 1)) >> 1, LOG) : null;
    // compute two-element sets of subspaces
    for(int i = 0; i < dbdim; i++) {
      for(int j = i + 1; j < dbdim; j++) {
        HiCSSubspace ts = new HiCSSubspace();
        ts.set(i);
        ts.set(j);
        calculateContrast(relation, ts, subspaceIndex, random);
        dDimensionalList.add(ts);
        if(prog != null) {
          prog.incrementProcessed(LOG);
        }
      }
    }
    if(prog != null) {
      prog.ensureCompleted(LOG);
    }

    IndefiniteProgress qprog = LOG.isVerbose() ? new IndefiniteProgress("Testing subspace candidates", LOG) : null;
    for(int d = 3; !dDimensionalList.isEmpty(); d++) {
      if(dprog != null) {
        dprog.setProcessed(d, LOG);
      }
      // result now contains all d-dimensional sets of subspaces

      ArrayList<HiCSSubspace> candidateList = new ArrayList<HiCSSubspace>(dDimensionalList.size());
      for (HiCSSubspace sub : dDimensionalList) {
        subspaceList.add(sub);
        candidateList.add(sub);
      }
      dDimensionalList.clear();
      // candidateList now contains the *m* best d-dimensional sets
      Collections.sort(candidateList, HiCSSubspace.SORT_BY_SUBSPACE);

      // TODO: optimize APRIORI style, by not even computing the bit set or?
      for(int i = 0; i < candidateList.size() - 1; i++) {
        for(int j = i + 1; j < candidateList.size(); j++) {
          HiCSSubspace set1 = candidateList.get(i);
          HiCSSubspace set2 = candidateList.get(j);

          HiCSSubspace joinedSet = new HiCSSubspace();
          joinedSet.or(set1);
          joinedSet.or(set2);
          if(joinedSet.cardinality() != d) {
            continue;
          }

          calculateContrast(relation, joinedSet, subspaceIndex, random);
          dDimensionalList.add(joinedSet);
          if(qprog != null) {
            qprog.incrementProcessed(LOG);
          }
        }
      }
      // Prune
      for(HiCSSubspace cand : candidateList) {
        for(HiCSSubspace nextSet : dDimensionalList) {
          if(nextSet.contrast > cand.contrast) {
            subspaceList.remove(cand);
            break;
          }
        }
      }
    }
    if(qprog != null) {
      qprog.setCompleted(LOG);
    }
    if(dprog != null) {
      dprog.setProcessed(dbdim, LOG);
      dprog.ensureCompleted(LOG);
    }
    return subspaceList;
  }

  /**
   * Calculates the actual contrast of a given subspace.
   * 
   * @param relation Relation to process
   * @param subspace Subspace
   * @param subspaceIndex Subspace indexes
   */
  private void calculateContrast(Relation<? extends NumberVector<?>> relation, HiCSSubspace subspace, ArrayList<ArrayDBIDs> subspaceIndex, Random random) {
    final int card = subspace.cardinality();
    final double alpha1 = Math.pow(alpha, (1.0 / card));
    final int windowsize = (int) (relation.size() * alpha1);
    final FiniteProgress prog = LOG.isDebugging() ? new FiniteProgress("Monte-Carlo iterations", m, LOG) : null;

    int retries = 0;
    double deviationSum = 0.0;
    for(int i = 0; i < m; i++) {
      // Choose a random set bit.
      int chosen = -1;
      for(int tmp = random.nextInt(card); tmp >= 0; tmp--) {
        chosen = subspace.nextSetBit(chosen + 1);
      }
      // initialize sample
      DBIDs conditionalSample = relation.getDBIDs();

      for(int j = subspace.nextSetBit(0); j >= 0; j = subspace.nextSetBit(j + 1)) {
        if(j == chosen) {
          continue;
        }
        ArrayDBIDs sortedIndices = subspaceIndex.get(j);
        ArrayModifiableDBIDs indexBlock = DBIDUtil.newArray(windowsize);
        // initialize index block
        DBIDArrayIter iter = sortedIndices.iter();
        iter.seek(random.nextInt(relation.size() - windowsize));
        for(int k = 0; k < windowsize; k++, iter.advance()) {
          indexBlock.add(iter); // select index block
        }

        conditionalSample = DBIDUtil.intersection(conditionalSample, indexBlock);
      }
      if(conditionalSample.size() < 10) {
        retries++;
        if(LOG.isDebugging()) {
          LOG.debug("Sample size very small. Retry no. " + retries);
        }
        if(retries >= MAX_RETRIES) {
          LOG.warning("Too many retries, for small samples: " + retries);
        }
        else {
          i--;
          continue;
        }
      }
      // Project conditional set
      double[] sampleValues = new double[conditionalSample.size()];
      {
        int l = 0;
        for (DBIDIter iter = conditionalSample.iter(); iter.valid(); iter.advance()) {
          sampleValues[l] = relation.get(iter).doubleValue(chosen);
          l++;
        }
      }
      // Project full set
      double[] fullValues = new double[relation.size()];
      {
        int l = 0;
        for (DBIDIter iter = subspaceIndex.get(chosen).iter(); iter.valid(); iter.advance()) {
          fullValues[l] = relation.get(iter).doubleValue(chosen);
          l++;
        }
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if(Double.isNaN(contrast)) {
        i--;
        LOG.warning("Contrast was NaN");
        continue;
      }
      deviationSum += contrast;
      if(prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(LOG);
    }
    subspace.contrast = deviationSum / m;
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
   * BitSet that holds a contrast value as field. Used for the representation of
   * a subspace in HiCS
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class HiCSSubspace extends BitSet {
    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The HiCS contrast value.
     */
    protected double contrast;

    /**
     * Constructor.
     */
    public HiCSSubspace() {
      super();
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append("[contrast=").append(contrast);
      for(int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
        buf.append(' ').append(i + 1);
      }
      buf.append(']');
      return buf.toString();
    }

    /**
     * Sort subspaces by their actual subspace.
     */
    public static final Comparator<HiCSSubspace> SORT_BY_CONTRAST_ASC = new Comparator<HiCSSubspace>() {
      @Override
      public int compare(HiCSSubspace o1, HiCSSubspace o2) {
        if(o1.contrast == o2.contrast) {
          return 0;
        }
        return o1.contrast > o2.contrast ? 1 : -1;
      }
    };

    /**
     * Sort subspaces by their actual subspace.
     */
    public static final Comparator<HiCSSubspace> SORT_BY_CONTRAST_DESC = new Comparator<HiCSSubspace>() {
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
    public static final Comparator<HiCSSubspace> SORT_BY_SUBSPACE = new Comparator<HiCSSubspace>() {
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
   * Parameterization class.
   * 
   * @author Jan Brusis
   * 
   * @apiviz.exclude
   * 
   * @param <V> vector type
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter that specifies the number of iterations in the Monte-Carlo
     * process of identifying high contrast subspaces.
     */
    public static final OptionID M_ID = OptionID.getOrCreateOptionID("hics.m", "The number of iterations in the Monte-Carlo processing.");

    /**
     * Parameter that determines the size of the test statistic during the
     * Monte-Carlo iteration.
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hics.alpha", "The discriminance value that determines the size of the test statistic .");

    /**
     * Parameter that specifies which outlier detection algorithm to use on the
     * resulting set of high contrast subspaces.
     */
    public static final OptionID ALGO_ID = OptionID.getOrCreateOptionID("hics.algo", "The Algorithm that performs the actual outlier detection on the resulting set of subspace");

    /**
     * Parameter that specifies which statistical test to use in order to
     * calculate the deviation of two given data samples.
     */
    public static final OptionID TEST_ID = OptionID.getOrCreateOptionID("hics.test", "The statistical test that is used to calculate the deviation of two data samples");

    /**
     * Parameter that specifies the candidate_cutoff.
     */
    public static final OptionID LIMIT_ID = OptionID.getOrCreateOptionID("hics.limit", "The threshold that determines how many d-dimensional subspace candidates to retain in each step of the generation");

    /**
     * Parameter that specifies the random seed.
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("hics.seed", "The random seed.");

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
     * Holds the value of {@link #LIMIT_ID}.
     */
    private int cutoff = 400;
    
    /**
     * Random generator.
     */
    private RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter mP = new IntParameter(M_ID, new GreaterConstraint(1), 50);
      if(config.grab(mP)) {
        m = mP.intValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 0.1);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      final ObjectParameter<OutlierAlgorithm> algoP = new ObjectParameter<OutlierAlgorithm>(ALGO_ID, OutlierAlgorithm.class, LOF.class);
      if(config.grab(algoP)) {
        outlierAlgorithm = algoP.instantiateClass(config);
      }

      final ObjectParameter<GoodnessOfFitTest> testP = new ObjectParameter<GoodnessOfFitTest>(TEST_ID, GoodnessOfFitTest.class, KolmogorovSmirnovTest.class);
      if(config.grab(testP)) {
        statTest = testP.instantiateClass(config);
      }

      final IntParameter cutoffP = new IntParameter(LIMIT_ID, new GreaterConstraint(1), 100);
      if(config.grab(cutoffP)) {
        cutoff = cutoffP.intValue();
      }

      final RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
}

    @Override
    protected HiCS<V> makeInstance() {
      return new HiCS<V>(m, alpha, outlierAlgorithm, statTest, cutoff, rnd);
    }
  }
}