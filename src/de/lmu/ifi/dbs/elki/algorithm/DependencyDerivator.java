package de.lmu.ifi.dbs.elki.algorithm;

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

import java.text.NumberFormat;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Dependency derivator computes quantitatively linear dependencies among
 * attributes of a given dataset based on a linear correlation PCA.
 * </p>
 * 
 * Reference: <br>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, A. Zimek: Deriving
 * Quantitative Dependencies for Correlation Clusters. <br>
 * In Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06),
 * Philadelphia, PA 2006. </p>
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("Dependency Derivator: Deriving numerical inter-dependencies on data")
@Description("Derives an equality-system describing dependencies between attributes in a correlation-cluster")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, A. Zimek", title = "Deriving Quantitative Dependencies for Correlation Clusters", booktitle = "Proc. 12th Int. Conf. on Knowledge Discovery and Data Mining (KDD '06), Philadelphia, PA 2006.", url = "http://dx.doi.org/10.1145/1150402.1150408")
public class DependencyDerivator<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm<V, D, CorrelationAnalysisSolution<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DependencyDerivator.class);

  /**
   * Flag to use random sample (use knn query around centroid, if flag is not
   * set).
   */
  public static final OptionID DEPENDENCY_DERIVATOR_RANDOM_SAMPLE = OptionID.getOrCreateOptionID("derivator.randomSample", "Flag to use random sample (use knn query around centroid, if flag is not set).");

  /**
   * Parameter to specify the threshold for output accuracy fraction digits,
   * must be an integer equal to or greater than 0.
   */
  public static final OptionID OUTPUT_ACCURACY_ID = OptionID.getOrCreateOptionID("derivator.accuracy", "Threshold for output accuracy fraction digits.");

  /**
   * Optional parameter to specify the threshold for the size of the random
   * sample to use, must be an integer greater than 0.
   * <p/>
   * Default value: the size of the complete dataset
   * </p>
   */
  public static final OptionID SAMPLE_SIZE_ID = OptionID.getOrCreateOptionID("derivator.sampleSize", "Threshold for the size of the random sample to use. " + "Default value is size of the complete dataset.");

  /**
   * Holds the value of {@link #SAMPLE_SIZE_ID}.
   */
  private final int sampleSize;

  /**
   * Holds the object performing the pca.
   */
  private final PCAFilteredRunner<V> pca;

  /**
   * Number format for output of solution.
   */
  public final NumberFormat NF;

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
   * @param sampleSize sample size
   * @param randomsample flag for random sampling
   */
  public DependencyDerivator(PrimitiveDistanceFunction<V, D> distanceFunction, NumberFormat nf, PCAFilteredRunner<V> pca, int sampleSize, boolean randomsample) {
    super(distanceFunction);
    this.NF = nf;
    this.pca = pca;
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
  public CorrelationAnalysisSolution<V> run(Database database, Relation<V> relation) throws IllegalStateException {
    if(logger.isVerbose()) {
      logger.verbose("retrieving database objects...");
    }
    V centroidDV = DatabaseUtil.centroid(relation);
    DBIDs ids;
    if(this.sampleSize > 0) {
      if(randomsample) {
        ids = DBIDUtil.randomSample(relation.getDBIDs(), this.sampleSize, 1l);
      }
      else {
        DistanceQuery<V, D> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
        KNNResult<D> queryResults = database.getKNNQuery(distanceQuery, this.sampleSize).getKNNForObject(centroidDV, this.sampleSize);
        ids = DBIDUtil.newHashSet(queryResults.asDBIDs());
      }
    }
    else {
      ids = relation.getDBIDs();
    }

    return generateModel(relation, ids, centroidDV);
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
    V centroidDV = DatabaseUtil.centroid(db, ids);
    return generateModel(db, ids, centroidDV);
  }

  /**
   * Runs the pca on the given set of IDs and for the given centroid.
   * 
   * @param db the database
   * @param ids the set of ids
   * @param centroidDV the centroid
   * @return a matrix of equations describing the dependencies
   */
  public CorrelationAnalysisSolution<V> generateModel(Relation<V> db, DBIDs ids, V centroidDV) {
    CorrelationAnalysisSolution<V> sol;
    if(logger.isDebuggingFine()) {
      logger.debugFine("PCA...");
    }

    PCAFilteredResult pcares = pca.processIds(ids, db);
    // Matrix weakEigenvectors =
    // pca.getEigenvectors().times(pca.selectionMatrixOfWeakEigenvectors());
    Matrix weakEigenvectors = pcares.getWeakEigenvectors();
    // Matrix strongEigenvectors =
    // pca.getEigenvectors().times(pca.selectionMatrixOfStrongEigenvectors());
    Matrix strongEigenvectors = pcares.getStrongEigenvectors();
    Vector centroid = centroidDV.getColumnVector();

    // TODO: what if we don't have any weak eigenvectors?
    if(weakEigenvectors.getColumnDimensionality() == 0) {
      sol = new CorrelationAnalysisSolution<V>(null, db, strongEigenvectors, weakEigenvectors, pcares.similarityMatrix(), centroid);
    }
    else {
      Matrix transposedWeakEigenvectors = weakEigenvectors.transpose();
      if(logger.isDebugging()) {
        StringBuilder log = new StringBuilder();
        log.append("Strong Eigenvectors:\n");
        log.append(FormatUtil.format(pcares.getEigenvectors().times(pcares.selectionMatrixOfStrongEigenvectors()), NF)).append('\n');
        log.append("Transposed weak Eigenvectors:\n");
        log.append(FormatUtil.format(transposedWeakEigenvectors, NF)).append('\n');
        log.append("Eigenvalues:\n");
        log.append(FormatUtil.format(pcares.getEigenvalues(), " , ", 2));
        logger.debugFine(log.toString());
      }
      Vector B = transposedWeakEigenvectors.times(centroid);
      if(logger.isDebugging()) {
        StringBuilder log = new StringBuilder();
        log.append("Centroid:\n").append(centroid).append('\n');
        log.append("tEV * Centroid\n");
        log.append(B);
        logger.debugFine(log.toString());
      }

      // +1 == + B.getColumnDimensionality()
      Matrix gaussJordan = new Matrix(transposedWeakEigenvectors.getRowDimensionality(), transposedWeakEigenvectors.getColumnDimensionality() + 1);
      gaussJordan.setMatrix(0, transposedWeakEigenvectors.getRowDimensionality() - 1, 0, transposedWeakEigenvectors.getColumnDimensionality() - 1, transposedWeakEigenvectors);
      gaussJordan.setCol(transposedWeakEigenvectors.getColumnDimensionality(), B);

      if(logger.isDebuggingFiner()) {
        logger.debugFiner("Gauss-Jordan-Elimination of " + FormatUtil.format(gaussJordan, NF));
      }

      double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
      double[][] we = transposedWeakEigenvectors.getArrayRef();
      double[] b = B.getArrayRef();
      System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());

      LinearEquationSystem lq = new LinearEquationSystem(a, b);
      lq.solveByTotalPivotSearch();

      sol = new CorrelationAnalysisSolution<V>(lq, db, strongEigenvectors, pcares.getWeakEigenvectors(), pcares.similarityMatrix(), centroid);

      if(logger.isDebuggingFine()) {
        StringBuilder log = new StringBuilder();
        log.append("Solution:\n");
        log.append("Standard deviation ").append(sol.getStandardDeviation());
        log.append(lq.equationsToString(NF.getMaximumFractionDigits()));
        logger.debugFine(log.toString());
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
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<V, D> {
    protected int outputAccuracy = 0;

    protected int sampleSize = 0;

    protected boolean randomSample = false;

    protected PCAFilteredRunner<V> pca = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configAccuracy(config);
      configSampleSize(config);
      configRandomSampleFlag(config);
      Class<PCAFilteredRunner<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
      pca = config.tryInstantiate(cls);
    }

    public void configRandomSampleFlag(Parameterization config) {
      Flag randomSampleF = new Flag(DEPENDENCY_DERIVATOR_RANDOM_SAMPLE);
      if(config.grab(randomSampleF)) {
        randomSample = randomSampleF.getValue();
      }
    }

    public void configSampleSize(Parameterization config) {
      IntParameter sampleSizeP = new IntParameter(SAMPLE_SIZE_ID, true);
      sampleSizeP.addConstraint(new GreaterConstraint(0));
      if(config.grab(sampleSizeP)) {
        sampleSize = sampleSizeP.getValue();
      }
    }

    public void configAccuracy(Parameterization config) {
      IntParameter outputAccuracyP = new IntParameter(OUTPUT_ACCURACY_ID, 4);
      outputAccuracyP.addConstraint(new GreaterEqualConstraint(0));
      if(config.grab(outputAccuracyP)) {
        outputAccuracy = outputAccuracyP.getValue();
      }
    }

    @Override
    protected DependencyDerivator<V, D> makeInstance() {
      NumberFormat NF = NumberFormat.getInstance(Locale.US);
      NF.setMaximumFractionDigits(outputAccuracy);
      NF.setMinimumFractionDigits(outputAccuracy);

      return new DependencyDerivator<V, D>(distanceFunction, NF, pca, sampleSize, randomSample);
    }
  }
}