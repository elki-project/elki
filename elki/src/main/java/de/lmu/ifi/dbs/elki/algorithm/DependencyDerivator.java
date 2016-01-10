package de.lmu.ifi.dbs.elki.algorithm;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;

import java.text.NumberFormat;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * Dependency derivator computes quantitatively linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA.
 * </p>
 *
 * <p>
 * Reference: <br>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, A. Zimek: Deriving
 * Quantitative Dependencies for Correlation Clusters. <br>
 * In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06),
 * Philadelphia, PA 2006.
 * </p>
 *
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
@Title("Dependency Derivator: Deriving numerical inter-dependencies on data")
@Description("Derives an equality-system describing dependencies between attributes in a correlation-cluster")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, A. Zimek", //
title = "Deriving Quantitative Dependencies for Correlation Clusters", //
booktitle = "Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06), Philadelphia, PA 2006.", //
url = "http://dx.doi.org/10.1145/1150402.1150408")
public class DependencyDerivator<V extends NumberVector> extends AbstractNumberVectorDistanceBasedAlgorithm<V, CorrelationAnalysisSolution<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DependencyDerivator.class);

  /**
   * Holds the value of {@link Parameterizer#SAMPLE_SIZE_ID}.
   */
  private final int sampleSize;

  /**
   * Holds the object performing the pca.
   */
  private final PCARunner pca;

  /**
   * Filter to select eigenvectors.
   */
  private final EigenPairFilter filter;

  /**
   * Number format for output of solution.
   */
  private final NumberFormat nf;

  /**
   * Flag for random sampling vs. kNN
   */
  private final boolean randomsample;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param nf Number format
   * @param pca PCA runner
   * @param filter Eigenvector filter
   * @param sampleSize sample size
   * @param randomsample flag for random sampling
   */
  public DependencyDerivator(NumberVectorDistanceFunction<? super V> distanceFunction, NumberFormat nf, PCARunner pca, EigenPairFilter filter, int sampleSize, boolean randomsample) {
    super(distanceFunction);
    this.nf = nf;
    this.pca = pca;
    this.filter = filter;
    this.sampleSize = sampleSize;
    this.randomsample = randomsample;
  }

  /**
   * Computes quantitatively linear dependencies among the attributes of the
   * given database based on a linear correlation PCA.
   *
   * @param database the database to run this DependencyDerivator on
   * @param relation the relation to use
   * @return the CorrelationAnalysisSolution computed by this
   *         DependencyDerivator
   */
  public CorrelationAnalysisSolution<V> run(Database database, Relation<V> relation) {
    if(LOG.isVerbose()) {
      LOG.verbose("retrieving database objects...");
    }
    Centroid centroid = Centroid.make(relation);
    V centroidDV = centroid.toVector(relation);
    DBIDs ids;
    if(this.sampleSize > 0) {
      if(randomsample) {
        ids = DBIDUtil.randomSample(relation.getDBIDs(), this.sampleSize, RandomFactory.DEFAULT);
      }
      else {
        DistanceQuery<V> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
        KNNList queryResults = database.getKNNQuery(distanceQuery, this.sampleSize)//
        .getKNNForObject(centroidDV, this.sampleSize);
        ids = DBIDUtil.newHashSet(queryResults);
      }
    }
    else {
      ids = relation.getDBIDs();
    }

    return generateModel(relation, ids, centroid.getArrayRef());
  }

  /**
   * Runs the pca on the given set of IDs. The centroid is computed from the
   * given ids.
   *
   * @param db the database
   * @param ids the set of ids
   * @return a matrix of equations describing the dependencies
   */
  public CorrelationAnalysisSolution<V> generateModel(Relation<V> db, DBIDs ids) {
    return generateModel(db, ids, Centroid.make(db, ids).getArrayRef());
  }

  /**
   * Runs the pca on the given set of IDs and for the given centroid.
   *
   * @param relation the database
   * @param ids the set of ids
   * @param centroid the centroid
   * @return a matrix of equations describing the dependencies
   */
  public CorrelationAnalysisSolution<V> generateModel(Relation<V> relation, DBIDs ids, double[] centroid) {
    CorrelationAnalysisSolution<V> sol;
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("PCA...");
    }

    SortedEigenPairs epairs = pca.processIds(ids, relation).getEigenPairs();
    int numstrong = filter.filter(epairs.eigenValues());
    PCAFilteredResult pcares = new PCAFilteredResult(epairs, numstrong, 1., 0.);

    // Matrix weakEigenvectors =
    // pca.getEigenvectors().times(pca.selectionMatrixOfWeakEigenvectors());
    Matrix weakEigenvectors = pcares.getWeakEigenvectors();
    // Matrix strongEigenvectors =
    // pca.getEigenvectors().times(pca.selectionMatrixOfStrongEigenvectors());
    Matrix strongEigenvectors = pcares.getStrongEigenvectors();

    // TODO: what if we don't have any weak eigenvectors?
    if(weakEigenvectors.getColumnDimensionality() == 0) {
      sol = new CorrelationAnalysisSolution<>(null, relation, strongEigenvectors, weakEigenvectors, pcares.similarityMatrix(), centroid);
    }
    else {
      Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
      if(LOG.isDebugging()) {
        StringBuilder log = new StringBuilder();
        log.append("Strong Eigenvectors:\n");
        FormatUtil.formatTo(log, pcares.getStrongEigenvectors().getArrayRef(), " [", "]\n", ", ", nf).append('\n');
        log.append("Transposed weak Eigenvectors:\n");
        FormatUtil.formatTo(log, transposedWeakEigenvectors.getArrayRef(), " [", "]\n", ", ", nf).append('\n');
        log.append("Eigenvalues:\n");
        log.append(FormatUtil.format(pcares.getEigenvalues(), ", ", nf));
        LOG.debugFine(log.toString());
      }
      double[] b = times(transposedWeakEigenvectors, centroid);
      if(LOG.isDebugging()) {
        StringBuilder log = new StringBuilder();
        log.append("Centroid:\n").append(centroid).append('\n');
        log.append("tEV * Centroid\n");
        log.append(FormatUtil.format(b));
        LOG.debugFine(log.toString());
      }

      // +1 == + B.getColumnDimensionality()
      Matrix gaussJordan = new Matrix(transposedWeakEigenvectors.getRowDimensionality(), transposedWeakEigenvectors.getColumnDimensionality() + 1);
      gaussJordan.setMatrix(0, transposedWeakEigenvectors.getRowDimensionality() - 1, 0, transposedWeakEigenvectors.getColumnDimensionality() - 1, transposedWeakEigenvectors);
      gaussJordan.setCol(transposedWeakEigenvectors.getColumnDimensionality(), b);

      if(LOG.isDebuggingFiner()) {
        LOG.debugFiner("Gauss-Jordan-Elimination of " + FormatUtil.format(gaussJordan.getArrayRef(), " [", "]\n", ", ", nf));
      }

      double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
      double[][] we = transposedWeakEigenvectors.getArrayRef();
      System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());

      LinearEquationSystem lq = new LinearEquationSystem(a, b);
      lq.solveByTotalPivotSearch();

      sol = new CorrelationAnalysisSolution<>(lq, relation, strongEigenvectors, pcares.getWeakEigenvectors(), pcares.similarityMatrix(), centroid);

      if(LOG.isDebuggingFine()) {
        StringBuilder log = new StringBuilder();
        log.append("Solution:\n");
        log.append("Standard deviation ").append(sol.getStandardDeviation());
        log.append(lq.equationsToString(nf.getMaximumFractionDigits()));
        LOG.debugFine(log.toString());
      }
    }
    return sol;
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
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractNumberVectorDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * Flag to use random sample (use knn query around centroid, if flag is not
     * set).
     */
    public static final OptionID DEPENDENCY_DERIVATOR_RANDOM_SAMPLE_ID = new OptionID("derivator.randomSample", "Flag to use random sample (use knn query around centroid, if flag is not set).");

    /**
     * Parameter to specify the threshold for output accuracy fraction digits,
     * must be an integer equal to or greater than 0.
     */
    public static final OptionID OUTPUT_ACCURACY_ID = new OptionID("derivator.accuracy", "Threshold for output accuracy fraction digits.");

    /**
     * Optional parameter to specify the threshold for the size of the random
     * sample to use, must be an integer greater than 0.
     * <p>
     * Default value: the size of the complete dataset
     * </p>
     */
    public static final OptionID SAMPLE_SIZE_ID = new OptionID("derivator.sampleSize", "Threshold for the size of the random sample to use. " + "Default value is size of the complete dataset.");

    /**
     * Output accuracy.
     */
    protected int outputAccuracy = 0;

    /**
     * Sample size.
     */
    protected int sampleSize = 0;

    /**
     * Flag to enable random sampling
     */
    protected boolean randomSample = false;

    /**
     * Class to compute PCA with
     */
    protected PCARunner pca = null;

    /**
     * Filter to select eigenvectors.
     */
    protected EigenPairFilter filter;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter outputAccuracyP = new IntParameter(OUTPUT_ACCURACY_ID, 4);
      outputAccuracyP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(outputAccuracyP)) {
        outputAccuracy = outputAccuracyP.getValue();
      }

      IntParameter sampleSizeP = new IntParameter(SAMPLE_SIZE_ID);
      sampleSizeP.setOptional(true);
      sampleSizeP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(sampleSizeP)) {
        sampleSize = sampleSizeP.getValue();
      }

      Flag randomSampleF = new Flag(DEPENDENCY_DERIVATOR_RANDOM_SAMPLE_ID);
      if(config.grab(randomSampleF)) {
        randomSample = randomSampleF.getValue();
      }
      ObjectParameter<PCARunner> pcaP = new ObjectParameter<>(PCARunner.Parameterizer.PCARUNNER_ID, PCARunner.class, PCARunner.class);
      if(config.grab(pcaP)) {
        pca = pcaP.instantiateClass(config);
      }
      ObjectParameter<EigenPairFilter> filterP = new ObjectParameter<>(EigenPairFilter.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class);
      if(config.grab(filterP)) {
        filter = filterP.instantiateClass(config);
      }
    }

    @Override
    protected DependencyDerivator<V> makeInstance() {
      NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setMaximumFractionDigits(outputAccuracy);
      nf.setMinimumFractionDigits(outputAccuracy);

      return new DependencyDerivator<>(distanceFunction, nf, pca, filter, sampleSize, randomSample);
    }
  }
}
