package de.lmu.ifi.dbs.varianceanalysis.ica;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.varianceanalysis.CompositeEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.GlobalPCA;
import de.lmu.ifi.dbs.varianceanalysis.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.zelki.ica.ICADataGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.*;

/**
 * Implementation of the FastICA algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FastICA extends AbstractParameterizable {

  private static NumberFormat NF = NumberFormat.getInstance(Locale.US);

  static {
    NF.setMinimumFractionDigits(4);
    NF.setMaximumFractionDigits(4);
  }

  /**
   * The approach.
   */
  public enum Approach {
    SYMMETRIC,
    DEFLATION
  }

  /**
   * Parameter for ic.
   */
  public static final String IC_P = "ic";

  /**
   * Description for parameter ic.
   */
  public static final String IC_D = "<int>the maximum number of independent components to be found.";

  /**
   * Parameter for initial unit matrix.
   */
  public static final String UNIT_F = "unit";

  /**
   * Description for parameter initial unit matrix.
   */
  public static final String UNIT_D = "flag that indicates that the unit matrix " +
                                      "is used as initial weight matrix. If this flag " +
                                      "is not set the initial weight matrix will be " +
                                      "generated randomly.";

  /**
   * Parameter for maxIter.
   */
  public static final String MAX_ITERATIONS_P = "maxIter";

  /**
   * Description for parameter maxIter.
   */
  public static final String MAX_ITERATIONS_D = "<int>the number of maximum iterations.";

  /**
   * Parameter for approach.
   */
  public static final String APPROACH_P = "app";

  /**
   * The default approach.
   */
  public static final Approach DEFAULT_APPROACH = Approach.DEFLATION;

  /**
   * Description for parameter approach.
   */
  public static final String APPROACH_D = "the approach to be used, available approaches are: [" +
                                          Approach.DEFLATION + "| " + Approach.SYMMETRIC + "]"
                                          + ". Default: " + DEFAULT_APPROACH + ")";

  /**
   * Parameter for contrastfunction g.
   */
  public static final String G_P = "g";

  /**
   * The default g.
   */
  public static final String DEFAULT_G = KurtosisBasedContrastFunction.class.getName();

  /**
   * Description for parameter g.
   */
  public static final String G_D = "the contrast function to be used to estimate negentropy "
                                   + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(ContrastFunction.class)
                                   + ". Default: " + DEFAULT_G;

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * The default epsilon.
   */
  public static final double DEFAULT_EPSILON = 0.001;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<double>a positive value defining the criterion for convergence of weight vector w_p: " +
                                         "if the difference of the values of w_p after two iterations " +
                                         "is less than or equal to epsilon. " +
                                         "Default: " + DEFAULT_EPSILON;

  /**
   * Option string for parameter alpha.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Description for parameter alpha.
   */
  public static final String ALPHA_D = "<double>a double between 0 and 1 specifying " +
                                       "the threshold for strong eigenvectors of the pca " +
                                       "performed as a preprocessing step: " +
                                       "the strong eigenvectors explain a " +
                                       "portion of at least alpha of the total variance " +
                                       "(default is alpha = " + PercentageEigenPairFilter.DEFAULT_ALPHA + ")";

  /**
   * The pca.
   */
  private GlobalPCA pca;

  /**
   * The input data.
   */
  private Matrix x0;

  /**
   * The reduced data after pca of x0.
   */
  private Matrix x1;

  /**
   * The centered x1 data.
   */
  private Matrix x2;

  /**
   * The whitened x2 data.
   */
  private Matrix x3;

  /**
   * The centroid of x1.
   */
  private Vector centroid1;

  /**
   * The whitening matrix.
   */
  private Matrix whiteningMatrix;

  /**
   * The dewhitening matrix.
   */
  private Matrix dewhiteningMatrix;

  /**
   * The mixing matrix.
   */
  private Matrix mixingMatrix;

  /**
   * The separating matrix.
   */
  private Matrix separatingMatrix;

  /**
   * The number of independent components to be found.
   */
  private int numICs;

  /**
   * The (current) weight matrix.
   */
  private Matrix weightMatrix;

  /**
   * The independent components
   */
  private Matrix ics;

  /**
   * The maximum number of iterations to be performed.
   */
  private int maximumIterations;

  /**
   * The approach to be used.
   */
  private Approach approach;

  /**
   * The contrast function;
   */
  private ContrastFunction contrastFunction;

  /**
   * The convergence criterion.
   */
  private double epsilon;

  /**
   * True, if the initial weight matrix is a unit matrix,
   * false if the initial weight matrix is generated randomly.
   */
  private boolean initialUnitWeightMatrix;

  /**
   * The alpha parameter for the preprocessing pca.
   */
  private double alpha;

  /**
   * Provides the fast ica algorithm.
   */
  public FastICA() {
    super();
    optionHandler.put(UNIT_F, new Flag(UNIT_F, UNIT_D));
    optionHandler.put(IC_P, new Parameter(IC_P, IC_D));
    optionHandler.put(MAX_ITERATIONS_P, new Parameter(MAX_ITERATIONS_P, MAX_ITERATIONS_D));
    optionHandler.put(APPROACH_P, new Parameter(APPROACH_P, APPROACH_D));
    optionHandler.put(G_P, new Parameter(G_P, G_D));
    optionHandler.put(EPSILON_P, new Parameter(EPSILON_P, EPSILON_D));
    optionHandler.put(ALPHA_P, new Parameter(ALPHA_P, ALPHA_D));
    this.debug = true;
  }

  /**
   * Runs the fast ica algorithm on the specified database.
   *
   * @param database the database containing the data vectors
   * @param verbose  flag that allows verbode messages
   */
  public void run(Database<RealVector> database, boolean verbose) {

    if (verbose) {
      verbose("data whitening");
    }
    whitenData(database);

    // set number of independent components to be found
    int dim = x3.getRowDimension();
    if (numICs > dim) {
      numICs = dim;
    }
    if (debug) {
      debugFine("\n numICs = " + numICs);
    }

    // initialize the weight matrix
    weightMatrix = new Matrix(dim, numICs);

    // determine the weights
    if (approach.equals(Approach.SYMMETRIC)) {
      symmetricOrthogonalization(dim, database.size());
    }
    else if (approach.equals(Approach.DEFLATION)) {
      deflationaryOrthogonalization(dim, database.size());
    }

    // recalculate mixing matrix
//    mixingMatrix = pca.getStrongEigenvectors().times(dewhiteningMatrix.times(weightMatrix));
//    separatingMatrix = weightMatrix.transpose().times(whiteningMatrix).times(pca.getStrongEigenvectors().transpose());
    mixingMatrix = dewhiteningMatrix.times(weightMatrix);
    separatingMatrix = weightMatrix.transpose().times(whiteningMatrix);

    ics = separatingMatrix.times(x2);

    for (int i = 0; i < ics.getColumnDimension(); i++) {
      Vector ic = ics.getColumnVector(i);
      ics.setColumn(i, ic.plus(centroid1));
    }

    ics = pca.getStrongEigenvectors().times(ics);

    System.out.println("strong " + pca.getStrongEigenvectors());

    generate(pca.getStrongEigenvectors().times(mixingMatrix), Util.centroid(inputMatrix(database)).getColumnPackedCopy(), "ic");
    generate(weightMatrix, Util.centroid(x3).getColumnPackedCopy(), "w");
    output(ics.transpose(), "ics");
    output(mixingMatrix.times(ics).transpose(), "x");

    if (debug) {
      StringBuffer msg = new StringBuffer();
//      msg.append("\nweight " + weightMatrix);
//      msg.append("\nmix " + mixingMatrix.toString(NF));
//      msg.append("\nsep " + separatingMatrix);
//      msg.append("\nics " + ics.transpose());
      debugFine(msg.toString());
    }
  }

  private void deflationaryOrthogonalization(int dimensionality, int size) {
    Progress progress = new Progress("Deflationary Orthogonalization ", numICs);

    for (int p = 0; p < numICs; p++) {
      progress.setProcessed(p);
      progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(progress), progress.getTask(), progress.status()));
      int iterations = 0;
      boolean converged = false;

      Vector w_p = initialUnitWeightMatrix ?
                   Vector.unitVector(dimensionality, p) :
                   Vector.randomNormalizedVector(dimensionality);

      while ((iterations < maximumIterations) && (!converged)) {
        // determine w_p
        Vector w_p_old = w_p.copy();
        w_p = updateWeight(w_p, dimensionality, size);

        // orthogonalize w_p
        Vector sum = new Vector(dimensionality);
        for (int j = 0; j < p; j++) {
          Vector w_j = weightMatrix.getColumnVector(j);
          sum = sum.plus(w_j.times(w_p.scalarProduct(w_j)));
        }
        w_p = w_p.minus(sum);
        w_p.normalize();

        // test if good approximation
        converged = isVectorConverged(w_p_old, w_p);
        iterations ++;

        if (debug) {
          debugFine("\nw_" + p + " " + w_p + "\n");
        }
        progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(progress) + " - " + iterations, progress.getTask(), progress.status()));

        generate(w_p, Util.centroid(x3).getColumnPackedCopy(), "w_" + p + iterations);

      }

      // write new vector to the matrix
      weightMatrix.setColumn(p, w_p);
    }
  }

  private void symmetricOrthogonalization(int dimensionality, int size) {
    Progress progress = new Progress("Symmetric Orthogonalization ", numICs);

    // choose initial values for w_p
    for (int p = 0; p < numICs; ++p) {
      Vector w_p = initialUnitWeightMatrix ?
                   Vector.unitVector(dimensionality, p) :
                   Vector.randomNormalizedVector(dimensionality);

      weightMatrix.setColumn(p, w_p);
    }
    // ortogonalize weight matrix
    weightMatrix = symmetricOrthogonalization();
    generate(weightMatrix, Util.centroid(x3).getColumnPackedCopy(), "w_0");

    int iterations = 0;
    boolean converged = false;
    while ((iterations < maximumIterations) && (!converged)) {
      Matrix w_old = weightMatrix.copy();
      for (int p = 0; p < numICs; p++) {
        Vector w_p = updateWeight(weightMatrix.getColumnVector(p), dimensionality, size);
        weightMatrix.setColumn(p, w_p);
      }
      // orthogonalize
      weightMatrix = symmetricOrthogonalization();
      System.out.println("w_" + (iterations + 1) + weightMatrix);
      generate(weightMatrix, Util.centroid(x3).getColumnPackedCopy(), "w_" + (iterations + 1));

      // test if good approximation
      converged = isMatrixConverged(w_old, weightMatrix);
      iterations++;
    }
  }

  private Matrix symmetricOrthogonalization() {
    Matrix W = weightMatrix.transpose();
    EigenvalueDecomposition decomp = new EigenvalueDecomposition(W.times(W.transpose()));
    Matrix E = decomp.getV();
    Matrix D = decomp.getD();
    for (int i = 0; i < D.getRowDimension(); i++) {
      D.set(i, i, 1.0 / Math.sqrt(D.get(i, i)));
    }

    W = E.times(D).times(E.transpose()).times(W);

//      W = E.transpose().times(W);
//      W = D.times(W);
//      W = E.times(W);

    return W.transpose();
  }

  private Vector updateWeight(Vector w_p, int dimensionality, int size) {
    // E(z*(g(w_p*z))
    Vector E_zg = new Vector(dimensionality);
    // E(g'(w_p*z))
    double E_gd = 0.0;

    for (int i = 0; i < size; i++) {
      Vector z = x3.getColumnVector(i);
      // w_p * z
      double wz = w_p.scalarProduct(z);
      // g(w_p * z)
      double g = contrastFunction.function(wz);
      // g'(w_p * z)
      double gd = contrastFunction.derivative(wz);

//      System.out.println("wz " +wz);
//      System.out.println("gd " +gd);
      E_zg = E_zg.plus(z.times(g));
      E_gd += gd;
    }

    // E(z*(g(w_p*z))
    E_zg = E_zg.times(1.0 / size);
    // E(g'(w_p*z)) * w_p
    E_gd /= size;
    Vector E_gdw = w_p.times(E_gd);

//    System.out.println("");
//    System.out.println("E_gd "+E_gd);
//    System.out.println("E_zg " + E_zg.toString(NF));
//    System.out.println("E_gdw " + E_gdw.toString(NF));

    // w_p = E_zg - E_gd * w_p
    w_p = E_zg.minus(E_gdw);
    return w_p;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // ics
    String icString = optionHandler.getOptionValue(IC_P);
    try {
      numICs = Integer.parseInt(icString);
      if (numICs <= 0)
        throw new WrongParameterValueException(IC_P, icString, IC_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(IC_P, icString, IC_D, e);
    }

    // initial mixing matrix
    initialUnitWeightMatrix = optionHandler.isSet(UNIT_F);

    // maximum iterations
    String maxIterString = optionHandler.getOptionValue(MAX_ITERATIONS_P);
    try {
      maximumIterations = Integer.parseInt(maxIterString);
      if (maximumIterations <= 0)
        throw new WrongParameterValueException(MAX_ITERATIONS_P, maxIterString, MAX_ITERATIONS_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MAX_ITERATIONS_P, maxIterString, MAX_ITERATIONS_D, e);
    }

    // approach
    if (optionHandler.isSet(APPROACH_P)) {
      String approachString = optionHandler.getOptionValue(APPROACH_P);
      if (approachString.equals(Approach.DEFLATION.toString())) {
        approach = Approach.DEFLATION;
      }
      else if (approachString.equals(Approach.SYMMETRIC.toString())) {
        approach = Approach.SYMMETRIC;
      }
      else throw new WrongParameterValueException(APPROACH_P, approachString, APPROACH_P);
    }
    else {
      approach = DEFAULT_APPROACH;
    }

    // contrast function
    String className;
    if (optionHandler.isSet(G_P)) {
      className = optionHandler.getOptionValue(G_P);
    }
    else {
      className = DEFAULT_G;
    }
    try {
      // noinspection unchecked
      contrastFunction = Util.instantiate(ContrastFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(G_P, className, G_D, e);
    }
    remainingParameters = contrastFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    // epsilon
    if (optionHandler.isSet(EPSILON_P)) {
      String epsilonString = optionHandler.getOptionValue(EPSILON_P);
      try {
        epsilon = Double.parseDouble(epsilonString);
        if (epsilon <= 0) {
          throw new WrongParameterValueException(EPSILON_P, epsilonString, EPSILON_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(EPSILON_P, epsilonString, EPSILON_D, e);
      }
    }
    else {
      epsilon = DEFAULT_EPSILON;
    }

    // alpha
    if (optionHandler.isSet(ALPHA_P)) {
      String alphaString = optionHandler.getOptionValue(ALPHA_P);
      try {
        alpha = Double.parseDouble(alphaString);
        if (alpha <= 0 || alpha > 1) {
          throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D, e);
      }
    }
    else {
      alpha = PercentageEigenPairFilter.DEFAULT_ALPHA;
    }

    // pca
    pca = new GlobalPCA();
    List<String> pcaParameters = new ArrayList<String>();
    pcaParameters.add(OptionHandler.OPTION_PREFIX + GlobalPCA.EIGENPAIR_FILTER_P);
    pcaParameters.add(CompositeEigenPairFilter.class.getName());
    pcaParameters.add(OptionHandler.OPTION_PREFIX + CompositeEigenPairFilter.FILTERS_P);
    pcaParameters.add(PercentageEigenPairFilter.class.getName() + "," + FirstNEigenPairFilter.class.getName());
    pcaParameters.add(OptionHandler.OPTION_PREFIX + PercentageEigenPairFilter.ALPHA_P);
    pcaParameters.add(Double.toString(alpha));
    pcaParameters.add(OptionHandler.OPTION_PREFIX + FirstNEigenPairFilter.N_P);
    pcaParameters.add(Integer.toString(numICs));
    pca.setParameters(pcaParameters.toArray(new String[pcaParameters.size()]));

    return remainingParameters;
  }

  private Matrix inputMatrix(Database<RealVector> database) {
    int dim = database.dimensionality();
    double[][] input = new double[database.size()][dim];

    int i = 0;
    for (Iterator<Integer> it = database.iterator(); it.hasNext(); i++) {
      RealVector o = database.get(it.next());
      for (int d = 1; d <= dim; d++) {
        input[i][d - 1] = o.getValue(d).doubleValue();
      }
    }

    return new Matrix(input).transpose();
  }

  /**
   * Returns true, if the convergence criterion for weighting vector wp is reached.
   *
   * @param wp_old the old value of wp
   * @param wp_new the new value of wp
   * @return true, if the scalar product between wp_old and wp_new
   *         is less than or equal to 1-epsilon
   */
  private boolean isVectorConverged(Vector wp_old, Vector wp_new) {
    double scalar = Math.abs(wp_old.scalarProduct(wp_new));
    System.out.println("scalar " + scalar + " " + (scalar >= 1 - epsilon));
    return scalar >= (1 - epsilon) && scalar <= (1 + epsilon);
  }

  /**
   * Returns true, if the convergence criterion for weighting matrix w is reached.
   *
   * @param w_old the old value of w
   * @param w_new the new value of w
   * @return true, if the convergence criterion for each column vector is reached.
   */
  private boolean isMatrixConverged(Matrix w_old, Matrix w_new) {
    for (int p = 0; p < w_old.getColumnDimension(); p++) {
      Vector wp_old = w_old.getColumnVector(p);
      Vector wp_new = w_new.getColumnVector(p);
      if (! isVectorConverged(wp_old, wp_new))
        return false;
    }
    return true;
  }

  /**
   * Calculates the power of a symmetric matrix.
   *
   * @param inMatrix the symmetric matrix
   * @param power    the power
   * @return the resulting matrix
   */
  private static Matrix power(Matrix inMatrix, double power) {
    EigenvalueDecomposition evd = new EigenvalueDecomposition(inMatrix);
    Matrix eigenVectors = evd.getV();
    Matrix eigenValues = evd.getD();

    int m = eigenValues.getRowDimension();
    for (int i = 0; i < m; ++i) {
      double ev = eigenValues.get(i, i);
      eigenValues.set(i, i, Math.pow(ev, power));
    }

    return eigenVectors.times(eigenValues).times(eigenVectors.transpose());
  }

  /**
   * Returns the resulting independent components.
   *
   * @return the resulting independent components
   */
  public Matrix getICVectors() {
    return ics;
  }

  /**
   * Returns the assumed mixing matrix.
   *
   * @return the assumed mixing matrix
   */
  public Matrix getMixingMatrix() {
    return mixingMatrix;
  }

  /**
   * Returns the assumed seperating matrix.
   *
   * @return the assumed seperating matrix
   */
  public Matrix getSeparatingMatrix() {
    return separatingMatrix;
  }

  /**
   * Returns the weight matrix.
   *
   * @return the weight matrix
   */
  public Matrix getWeightMatrix() {
    return weightMatrix;
  }

  /**
   * Performs in a preprocessing step a pca on the data and afterwards the data whitening.
   *
   * @param database the database storing the vector objects
   */
  private void whitenData(Database<RealVector> database) {
    // perform a pca
    x0 = inputMatrix(database);
    pca.run(x0);
    x1 = pca.getStrongEigenvectors().transpose().times(x0);

    // center reduced data
    centroid1 = Util.centroid(x1);
    x2 = new Matrix(x1.getRowDimension(), x1.getColumnDimension());
    System.out.println("centroid " + centroid1);
    for (int i = 0; i < x1.getColumnDimension(); i++) {
      x2.setColumn(i, x1.getColumnVector(i).minus(centroid1));
    }
    System.out.println("centroid_new " + Util.centroid(x2));

    // whiten data
    Matrix cov = Util.covarianceMatrix(x2);
    EigenvalueDecomposition evd = cov.eig();
    // eigen vectors
    Matrix E = evd.getV();
    // eigenvalues ^-0.5
    Matrix D_inv_sqrt = evd.getD().copy();
    for (int i = 0; i < D_inv_sqrt.getRowDimension(); i++) {
      D_inv_sqrt.set(i, i, 1.0 / Math.sqrt(D_inv_sqrt.get(i, i)));
    }
    // eigenvalue ^1/2
    Matrix D_sqrt = evd.getD().copy();
    for (int i = 0; i < D_sqrt.getRowDimension(); i++) {
      D_sqrt.set(i, i, Math.sqrt(D_sqrt.get(i, i)));
    }

//    whiteningMatrix = D_inv_sqrt.times(E.transpose());
//    dewhiteningMatrix = E.times(D_sqrt);

    whiteningMatrix = E.times(D_inv_sqrt.times(E.transpose()));
    dewhiteningMatrix = E.times(D_sqrt).times(E.transpose());

    x3 = whiteningMatrix.times(x2);

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nWHITENING MATRIX: " + whiteningMatrix.dimensionInfo());
      msg.append("\n" + whiteningMatrix.toString(NF));
      msg.append("\nDEWHITENING MATRIX: " + dewhiteningMatrix.dimensionInfo());
      msg.append("\n" + dewhiteningMatrix.toString(NF));
//      msg.append("\nINPUT MATRIX: " + x.dimensionInfo());
//      msg.append("\n" + x.transpose().toString(NF));
//      msg.append("\nWHITENED MATRIX: " + whitenedVectors.dimensionInfo());
//      msg.append("\n" + whitenedVectors.transpose().toString(NF));
      debugFine(msg.toString());

      output(x0.transpose(), "x0");
      output(x1.transpose(), "x1");
      output(x2.transpose(), "x2");
      output(x3.transpose(), "x3");
    }
  }

  private void output(Matrix m, String name) {
    try {
      PrintStream printStream = new PrintStream(new FileOutputStream(name));
      printStream.println("# " + m.dimensionInfo());
      for (int i = 0; i < m.getRowDimension(); i++) {
        for (int j = 0; j < m.getColumnDimension(); j++) {
          printStream.print(m.get(i, j));
          printStream.print(" ");
        }
        printStream.println("");
      }
      printStream.flush();
      printStream.close();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  private void generate(Matrix m, double[] p, String name) {
    double[] min = new double[p.length];
    double[] max = new double[p.length];

    Arrays.fill(min, -100);
    Arrays.fill(max, 100);

    File file = new File(name);
    if (file.exists()) file.delete();

    for (int i = 0; i < m.getColumnDimension(); i++)
      ICADataGenerator.runGenerator(100, p, new double[][]{m.getColumnVector(i).getColumnPackedCopy()}, name + i, min, max, 0, name);
  }


}
