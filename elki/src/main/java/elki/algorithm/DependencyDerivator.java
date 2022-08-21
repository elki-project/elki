/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.algorithm;

import static elki.math.linearalgebra.VMath.*;
import static elki.utilities.io.FormatUtil.format;
import static elki.utilities.io.FormatUtil.formatTo;

import java.text.NumberFormat;
import java.util.Locale;

import elki.Algorithm;
import elki.data.NumberVector;
import elki.data.model.CorrelationAnalysisSolution;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.Centroid;
import elki.math.linearalgebra.LinearEquationSystem;
import elki.math.linearalgebra.pca.PCAFilteredResult;
import elki.math.linearalgebra.pca.PCAResult;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

/**
 * Dependency derivator computes quantitatively linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger,
 * Arthur Zimek<br>
 * Deriving Quantitative Dependencies for Correlation Clusters<br>
 * Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06)
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
@Title("Dependency Derivator: Deriving numerical inter-dependencies on data")
@Description("Derives an equality-system describing dependencies between attributes in a correlation-cluster")
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger, Arthur Zimek", //
    title = "Deriving Quantitative Dependencies for Correlation Clusters", //
    booktitle = "Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06)", //
    url = "https://doi.org/10.1145/1150402.1150408", //
    bibkey = "DBLP:conf/kdd/AchtertBKKZ06")
@Priority(Priority.DEFAULT - 5) // Mostly used inside others, not standalone
public class DependencyDerivator<V extends NumberVector> implements Algorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DependencyDerivator.class);

  /**
   * Distance function used.
   */
  private NumberVectorDistance<? super V> distance;

  /**
   * The number of samples to draw.
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
   * @param distance distance function
   * @param nf Number format
   * @param pca PCA runner
   * @param filter Eigenvector filter
   * @param sampleSize sample size
   * @param randomsample flag for random sampling
   */
  public DependencyDerivator(NumberVectorDistance<? super V> distance, NumberFormat nf, PCARunner pca, EigenPairFilter filter, int sampleSize, boolean randomsample) {
    super();
    this.distance = distance;
    this.nf = nf;
    this.pca = pca;
    this.filter = filter;
    this.sampleSize = sampleSize;
    this.randomsample = randomsample;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Computes quantitatively linear dependencies among the attributes of the
   * given database based on a linear correlation PCA.
   *
   * @param relation the relation to process
   * @return the CorrelationAnalysisSolution computed by this
   *         DependencyDerivator
   */
  public CorrelationAnalysisSolution run(Relation<V> relation) {
    if(LOG.isVerbose()) {
      LOG.verbose("retrieving database objects...");
    }
    Centroid centroid = Centroid.make(relation, relation.getDBIDs());
    NumberVector.Factory<V> factory = RelationUtil.getNumberVectorFactory(relation);
    V centroidDV = factory.newNumberVector(centroid.getArrayRef());
    DBIDs ids;
    if(sampleSize == 0) {
      ids = relation.getDBIDs();
    }
    else if(randomsample) {
      ids = DBIDUtil.randomSample(relation.getDBIDs(), sampleSize, RandomFactory.DEFAULT);
    }
    else {
      ids = new QueryBuilder<>(relation, distance).cheapOnly().kNNByObject(sampleSize) //
          .getKNN(centroidDV, sampleSize);
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
  public CorrelationAnalysisSolution generateModel(Relation<V> db, DBIDs ids) {
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
  public CorrelationAnalysisSolution generateModel(Relation<V> relation, DBIDs ids, double[] centroid) {
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("PCA...");
    }
    PCAResult epairs = pca.processIds(ids, relation);
    int numstrong = filter.filter(epairs.getEigenvalues());
    PCAFilteredResult pcares = new PCAFilteredResult(epairs.getEigenPairs(), numstrong, 1., 0.);

    double[][] transposedWeakEigenvectors = pcares.getWeakEigenvectors();
    double[][] transposedStrongEigenvectors = pcares.getStrongEigenvectors();

    // TODO: what if we don't have any weak eigenvectors?
    if(transposedWeakEigenvectors.length == 0) {
      return new CorrelationAnalysisSolution(null, relation, transpose(transposedStrongEigenvectors), new double[0][], pcares.similarityMatrix(), centroid);
    }
    // double[][] transposedWeakEigenvectors = transpose(weakEigenvectors);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder(1000);
      formatTo(msg.append("Strong Eigenvectors:\n"), transposedStrongEigenvectors, " [", "]\n", ", ", nf);
      formatTo(msg.append("\nWeak Eigenvectors:\n"), transposedWeakEigenvectors, " [", "]\n", ", ", nf);
      formatTo(msg.append("\nEigenvalues:\n"), pcares.getEigenvalues(), ", ", nf);
      LOG.debugFine(msg.toString());
    }
    double[] b = times(transposedWeakEigenvectors, centroid);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder(1000);
      formatTo(msg.append("Centroid:\n"), centroid, ", ", nf);
      formatTo(msg.append("\ntEV * Centroid\n"), b, ", ", nf);
      LOG.debugFine(msg.toString());
    }

    // +1 == + B[0].length
    double[][] gaussJordan = new double[transposedWeakEigenvectors.length][transposedWeakEigenvectors[0].length + 1];
    setMatrix(gaussJordan, 0, transposedWeakEigenvectors.length, 0, transposedWeakEigenvectors[0].length, transposedWeakEigenvectors);
    setCol(gaussJordan, transposedWeakEigenvectors[0].length, b);

    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner("Gauss-Jordan-Elimination of " + format(gaussJordan, " [", "]\n", ", ", nf));
    }
    LinearEquationSystem lq = new LinearEquationSystem(copy(transposedWeakEigenvectors), b);
    lq.solveByTotalPivotSearch();

    CorrelationAnalysisSolution sol = new CorrelationAnalysisSolution(lq, relation, transpose(transposedStrongEigenvectors), transpose(transposedWeakEigenvectors), pcares.similarityMatrix(), centroid);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine(new StringBuilder().append("Solution:\n") //
          .append("Standard deviation ").append(sol.getStandardDeviation()) //
          .append(lq.equationsToString(nf.getMaximumFractionDigits())).toString());
    }
    return sol;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
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
     */
    public static final OptionID SAMPLE_SIZE_ID = new OptionID("derivator.sampleSize", "Threshold for the size of the random sample to use. " + "Default value is size of the complete dataset.");

    /**
     * The distance function to use.
     */
    protected NumberVectorDistance<? super V> distance;

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
    public void configure(Parameterization config) {
      new ObjectParameter<NumberVectorDistance<? super V>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, NumberVectorDistance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(OUTPUT_ACCURACY_ID, 4) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> outputAccuracy = x);
      new IntParameter(SAMPLE_SIZE_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> sampleSize = x);
      new Flag(DEPENDENCY_DERIVATOR_RANDOM_SAMPLE_ID).grab(config, x -> randomSample = x);
      new ObjectParameter<PCARunner>(PCARunner.Par.PCARUNNER_ID, PCARunner.class, PCARunner.class) //
          .grab(config, x -> pca = x);
      new ObjectParameter<EigenPairFilter>(EigenPairFilter.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class) //
          .grab(config, x -> filter = x);
    }

    @Override
    public DependencyDerivator<V> make() {
      NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setMaximumFractionDigits(outputAccuracy);
      nf.setMinimumFractionDigits(outputAccuracy);

      return new DependencyDerivator<>(distance, nf, pca, filter, sampleSize, randomSample);
    }
  }
}
