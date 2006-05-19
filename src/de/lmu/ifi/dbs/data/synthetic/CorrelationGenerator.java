package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.wrapper.StandAloneWrapper;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Provides automatic generation of hyperplanes of arbitrary correlation dimensionalities.
 *
 * todo: comment all methods
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CorrelationGenerator extends StandAloneWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * A pattern defining a comma.
   */
  public static final Pattern COMMA_SPLIT = Pattern.compile(",");

  /**
   * A pattern defining a |.
   */
  public static final Pattern VECTOR_SPLIT = Pattern.compile(":");

  static {
    OUTPUT_D = "<filename>the file to write the generated correlation hyperplane in, " +
               "if the file already exists, the generated points will be appended to this file.";
  }

  /**
   * Parameter for dimensionality.
   */
  public static final String DIM_P = "dim";

  /**
   * Description for parameter dim.
   */
  public static final String DIM_D = "<int>the dimensionality of the feature space.";

  /**
   * Parameter for correlation dimensionality.
   */
  public static final String CORRDIM_P = "corrdim";

  /**
   * Description for parameter corrdim.
   */
  public static final String CORRDIM_D = "<int>the correlation dimensionality of the correlation hyperplane.";

  /**
   * Parameter for minimum value.
   */
  public static final String MIN_P = "minima";

  /**
   * The default value for min.
   */
  public static final double MIN_DEFAULT = 0;

  /**
   * Description for parameter min.
   */
  public static final String MIN_D = "<min_1,...,min_d>a comma seperated list of the coordinates of the minimum " +
                                     "value in each dimension, default is " + MIN_DEFAULT + " in each dimension";

  /**
   * Parameter for maximum value.
   */
  public static final String MAX_P = "maxima";

  /**
   * The default value for max.
   */
  public static final double MAX_DEFAULT = 1;

  /**
   * Description for parameter max.
   */
  public static final String MAX_D = "<max_1,...,max_d>a comma seperated list of the coordinates of the maximum " +
                                     "value in each dimension, default is " + MAX_DEFAULT + " in each dimension";

  /**
   * Parameter for number of points.
   */
  public static final String NUMBER_P = "number";

  /**
   * Description for parameter number.
   */
  public static final String NUMBER_D = "<int>the (positive) number of points in the correlation hyperplane.";

  /**
   * Parameter for model point.
   */
  public static final String POINT_P = "point";

  /**
   * Description for parameter point.
   */
  public static final String POINT_D = "<p_1,...,p_d>a comma seperated list of the coordinates of the model point, " +
                                       "default is the centroid of the defined feature space.";

  /**
   * Parameter for basis.
   */
  public static final String BASIS_P = "basis";

  /**
   * Description for parameter basis.
   */
  public static final String BASIS_D = "<b_11,...,b_1d:...:b_c1,...,b_cd>a list of basis vectors of the correlation hyperplane, " +
                                       "where c denotes the correlation dimensionality and d the dimensionality of the " +
                                       "feature space. Each basis vector is separated by :, the coordinates within " +
                                       "the basis vectors are seperated by a comma. If no basis is specified, the " +
                                       "basis vectors are generated randomly.";

  /**
   * Parameter for jitter.
   */
  public static final String JITTER_P = "jitter";

  /**
   * The default value for jitter.
   */
  public static final double JITTER_DEFAULT = 0;

  /**
   * Description for parameter jitter.
   */
  public static final String JITTER_D = "<double>maximum percentage [0..1] of jitter in each dimension, " +
                                        "default is " + JITTER_DEFAULT + ".";

  /**
   * Parameter for label.
   */
  public static final String LABEL_P = "label";

  /**
   * Description for parameter label.
   */
  public static final String LABEL_D = "<string>a label specifiying the correlation hyperplane, " +
                                       "default is no label.";


  /**
   * The minimum value in each dimension.
   */
  private double[] min;

  /**
   * The maximum value in each dimension.
   */
  private double[] max;

  /**
   * The number of points to be generated.
   */
  private int number;

  /**
   * The model point.
   */
  private Matrix point;

  /**
   * The basis vectors.
   */
  private Matrix basis;

  /**
   * The maximum percentage of jitter in each dimension.
   */
  private double jitter;

  /**
   * The standard deviation of jitter.
   */
  private double jitter_std;

  /**
   * Label for outpout.
   */
  private String label;

  /**
   * Number Formatter for output.
   */
  private static final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * The random generator.
   */
  private static Random RANDOM = new Random();

  /**
   * Creates a new correlation generator that provides automatic generation
   * of hyperplanes of arbitrary correlation dimensionalities.
   */
  public CorrelationGenerator() {
    super();
    parameterToDescription.put(DIM_P + OptionHandler.EXPECTS_VALUE, DIM_D);
    parameterToDescription.put(CORRDIM_P + OptionHandler.EXPECTS_VALUE, CORRDIM_D);
    parameterToDescription.put(MIN_P + OptionHandler.EXPECTS_VALUE, MIN_D);
    parameterToDescription.put(MAX_P + OptionHandler.EXPECTS_VALUE, MAX_D);
    parameterToDescription.put(NUMBER_P + OptionHandler.EXPECTS_VALUE, NUMBER_D);
    parameterToDescription.put(POINT_P + OptionHandler.EXPECTS_VALUE, POINT_D);
    parameterToDescription.put(BASIS_P + OptionHandler.EXPECTS_VALUE, BASIS_D);
    parameterToDescription.put(JITTER_P + OptionHandler.EXPECTS_VALUE, JITTER_D);
    parameterToDescription.put(LABEL_P + OptionHandler.EXPECTS_VALUE, LABEL_D);

    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
    CorrelationGenerator wrapper = new CorrelationGenerator();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (Exception e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Runs the wrapper with the specified arguments.
   *
   * @param args parameter list
   */
  public void run(String[] args) throws UnableToComplyException, ParameterException {
    try {
      super.run(args);
      File outputFile = new File(getOutput());

      if (outputFile.exists()) {
        if (isVerbose()) {
          System.out.println("The file " + outputFile + " already exists, " +
                             "the generator result will be appended.");
        }
      }

      setParameters();
      PrintStream outStream = new PrintStream(new FileOutputStream(outputFile, true));
      generateCorrelation(outStream);
    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e.getMessage(), e);
    }

  }

  /**
   * Sets the parameters.
   */
  private void setParameters() throws UnusedParameterException, NoParameterValueException, WrongParameterValueException {
    // dim
    int dim;
    String dimString = optionHandler.getOptionValue(DIM_P);
    try {
      dim = Integer.parseInt(dimString);
      if (dim <= 0)
        throw new WrongParameterValueException(DIM_P, dimString, DIM_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(DIM_P, dimString, DIM_D, e);
    }

    // corrDim;
    int corrDim;
    String corrdimString = optionHandler.getOptionValue(CORRDIM_P);
    try {
      corrDim = Integer.parseInt(corrdimString);
      if (corrDim <= 0)
        throw new WrongParameterValueException(CORRDIM_P, corrdimString, CORRDIM_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(CORRDIM_P, corrdimString, CORRDIM_D, e);
    }

    // corrDim < dim?
    if (corrDim > dim) {
      throw new WrongParameterValueException("Parameter " + CORRDIM_P + " > " + DIM_P + "!");
    }

    // min
    min = new double[dim];
    if (optionHandler.isSet(MIN_P)) {
      String minString = optionHandler.getOptionValue(MIN_P);
      String[] minima = COMMA_SPLIT.split(minString);
      if (minima.length != dim)
        throw new WrongParameterValueException("Value of parameter " + MIN_P + " has not the specified dimensionality  " + DIM_P + " = " + dim);

      for (int i = 0; i < dim; i++) {
        try {
          min[i] = Double.parseDouble(minima[i]);
        }
        catch (NumberFormatException e) {
          throw new WrongParameterValueException(MIN_P, minString, MIN_D);
        }
      }
    }
    else {
      Arrays.fill(min, MIN_DEFAULT);
    }

    // max
    max = new double[dim];
    if (optionHandler.isSet(MAX_P)) {
      String maxString = optionHandler.getOptionValue(MAX_P);
      String[] maxima = COMMA_SPLIT.split(maxString);
      if (maxima.length != dim)
        throw new WrongParameterValueException("Value of parameter " + MAX_P + " has not the specified dimensionality  " + DIM_P + " = " + dim);

      for (int i = 0; i < dim; i++) {
        try {
          max[i] = Double.parseDouble(maxima[i]);
        }
        catch (NumberFormatException e) {
          throw new WrongParameterValueException(MAX_P, maxString, MAX_D);
        }
      }
    }
    else {
      Arrays.fill(max, MAX_DEFAULT);
    }

    // min < max?
    for (int i = 0; i < dim; i++) {
      if (min[i] >= max[i]) {
        throw new WrongParameterValueException("Parameter " + MIN_P + " > " + MAX_P + "!");
      }
    }

    // number of points
    String numberString = optionHandler.getOptionValue(NUMBER_P);
    try {
      number = Integer.parseInt(numberString);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(NUMBER_P, numberString, NUMBER_D, e);
    }
    if (number <= 0) {
      throw new WrongParameterValueException(NUMBER_P, numberString, NUMBER_D);
    }

    // model point
    if (optionHandler.isSet(POINT_P)) {
      String pointString = optionHandler.getOptionValue(POINT_P);
      String[] pointCoordinates = COMMA_SPLIT.split(pointString);
      if (pointCoordinates.length != dim)
        throw new WrongParameterValueException("Value of parameter " + POINT_P + " has not the specified dimensionality  " + DIM_P + " = " + dim);

      double[][] p = new double[dim][];
      for (int i = 0; i < dim; i++) {
        try {
          p[i] = new double[]{Double.parseDouble(pointCoordinates[i])};
        }
        catch (NumberFormatException e) {
          throw new WrongParameterValueException(POINT_P, pointString, POINT_D);
        }
      }
      point = new Matrix(p);
    }
    else {
      point = centroid(dim);
    }

    // basis
    if (optionHandler.isSet(BASIS_P)) {
      String basisString = optionHandler.getOptionValue(BASIS_P);
      String[] basisVectors = VECTOR_SPLIT.split(basisString);
      if (basisVectors.length != corrDim) {
        throw new WrongParameterValueException("Value of parameter " + BASIS_P + " has not the specified dimensionality  " +
                                               CORRDIM_P + " = " + corrDim);
      }

      double[][] b = new double[dim][corrDim];
      for (int c = 0; c < corrDim; c++) {
        String[] basisCoordinates = COMMA_SPLIT.split(basisVectors[c]);
        if (basisCoordinates.length != dim)
          throw new WrongParameterValueException("Value of parameter " + BASIS_P + " has not the specified dimensionality  " + DIM_P + " = " + dim);

        for (int d = 0; d < dim; d++) {
          try {
            b[d][c] = Double.parseDouble(basisCoordinates[d]);
          }
          catch (NumberFormatException e) {
            throw new WrongParameterValueException(BASIS_P, basisString, BASIS_D);
          }
        }
      }
      basis = new Matrix(b);
    }
    else {
      basis = correlationBasis(dim, corrDim);
    }

    // jitter
    if (optionHandler.isSet(JITTER_P)) {
      String jitterString = optionHandler.getOptionValue(JITTER_P);
      try {
        jitter = Double.parseDouble(jitterString);
        if (jitter < 0 || jitter > 1) {
          throw new WrongParameterValueException(JITTER_P, jitterString, JITTER_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(JITTER_P, jitterString, JITTER_D, e);
      }
    }
    else {
      jitter = JITTER_DEFAULT;
    }

    // jitter std
    jitter_std = 0;
    for (int d = 0; d < dim; d++) {
      jitter_std = Math.max(jitter * (max[d] - min[d]), jitter_std);
    }

    // label
    if (optionHandler.isSet(LABEL_P)) {
      label = optionHandler.getOptionValue(LABEL_P);
    }
    else label = "";
  }

  /**
   * Generates a correlation hyperplane according to the specified parameters.
   *
   * @param outStream the output stream to write into
   */
  private void generateCorrelation(PrintStream outStream) {
    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nbasis");
      msg.append(basis.toString(NF));
      msg.append("\npoint");
      msg.append(point.toString(NF));
      logger.fine(msg.toString());
    }

    if (point.getRowDimension() != basis.getRowDimension())
      throw new IllegalArgumentException("point.getRowDimension() != basis.getRowDimension()!");

    if (point.getColumnDimension() != 1)
      throw new IllegalArgumentException("point.getColumnDimension() != 1!");

    if (! inMinMax(point))
      throw new IllegalArgumentException("point not in min max!");

    if (basis.getColumnDimension() == basis.getRowDimension()) {
      generateNoise(outStream);
      return;
    }

    // determine the dependency
    Dependency dependency = determineDependency(point, basis);
    if (isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append(dependency.toString());
      logger.info(msg.toString());
    }

    Matrix b = dependency.basisVectors;

    List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(number);
    while (featureVectors.size() != number) {
      Matrix featureVector = generateCorrelation(point, b);
      double distance = distance(featureVector, point, b);
      if (distance > 1E-13 && isVerbose())
        logger.info("distance " + distance);
      if (jitter != 0) {
        featureVector = jitter(featureVector, dependency.normalVectors);
      }
      if (inMinMax(featureVector)) {
        featureVectors.add(new DoubleVector(featureVector));
      }
    }

    double std = standardDeviation(featureVectors, point, b);
    if (isVerbose()) {
      logger.info("standard deviation " + std + "\n");
    }
    output(outStream, featureVectors, dependency.dependency, std);
  }

  private Dependency determineDependency(final Matrix point, final Matrix basis) {
    StringBuffer msg = new StringBuffer();

    // orthonormal basis of subvectorspace U
    Matrix orthonormalBasis_U = orthonormalize(basis);
    Matrix completeVectors = completeBasis(orthonormalBasis_U);
    if (DEBUG) {
      msg.append("\npoint ").append(point.toString(NF));
      msg.append("\nbasis ").append(basis.toString(NF));
      msg.append("\northonormal basis ").append(orthonormalBasis_U.toString(NF));
      msg.append("\ncomplete vectors ").append(completeVectors.toString(NF));
      logger.fine(msg.toString());
    }

    // orthonormal basis of vectorspace V
    Matrix basis_V = appendColumn(orthonormalBasis_U, completeVectors);
    basis_V = orthonormalize(basis_V);
    if (DEBUG) {
      logger.fine("basis V " + basis_V.toString(NF));
    }

    // normal vectors of U
    Matrix normalVectors_U = basis_V.getMatrix(0, basis_V.getRowDimension() - 1,
                                               basis.getColumnDimension(),
                                               basis.getRowDimension() - basis.getColumnDimension() + basis.getColumnDimension() - 1);
    if (DEBUG) {
      logger.fine("normal vector U " + normalVectors_U.toString(NF));
    }
    Matrix transposedNormalVectors = normalVectors_U.transpose();
    if (DEBUG) {
      logger.fine("tNV " + transposedNormalVectors.toString(NF));
      logger.fine("point " + point.toString(NF));
    }

    // gauss jordan
    Matrix B = transposedNormalVectors.times(point);
    if (DEBUG) {
      logger.fine("B " + B.toString(NF));
    }
    Matrix gaussJordan = new Matrix(transposedNormalVectors.getRowDimension(), transposedNormalVectors.getColumnDimension() + B.getColumnDimension());
    gaussJordan.setMatrix(0, transposedNormalVectors.getRowDimension() - 1, 0, transposedNormalVectors.getColumnDimension() - 1, transposedNormalVectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimension() - 1, transposedNormalVectors.getColumnDimension(), gaussJordan.getColumnDimension() - 1, B);


    double[][] a = new double[transposedNormalVectors.getRowDimension()][transposedNormalVectors.getColumnDimension()];
    double[][] we = transposedNormalVectors.getArray();
    double[] b = B.getColumn(0).getRowPackedCopy();
    System.arraycopy(we, 0, a, 0, transposedNormalVectors.getRowDimension());

//    System.out.println("a "+new Matrix(a).toString(NF));
//    System.out.println("b "+Util.format(b, ",", 4));

    LinearEquationSystem lq = new LinearEquationSystem(a, b);
    lq.solveByTotalPivotSearch();
    Dependency dependency = new Dependency(orthonormalBasis_U, normalVectors_U, lq);
//    System.out.println("solution " + lq.equationsToString(NF.getMinimumFractionDigits(), NF.getMaximumFractionDigits()));
//    System.out.println("dep " + dependency);
    return dependency;
  }

  private Matrix generateCorrelation(Matrix point, Matrix basis) {
    Matrix featureVector = point.copy();
    for (int i = 0; i < basis.getColumnDimension(); i++) {
//      System.out.println("   d " + distance(featureVector, point, basis));
//      double lambda_i = RANDOM.nextDouble() * (0.5 * Math.sqrt(point.getRowDimension())) / point.getRowDimension();
//      double lambda_i = RANDOM.nextDouble();
      double lambda_i = RANDOM.nextGaussian();
      if (RANDOM.nextBoolean()) lambda_i *= -1;
      Matrix b_i = basis.getColumn(i);
      featureVector = featureVector.plus(b_i.times(lambda_i));

    }
    return featureVector;
  }

  private Matrix jitter(Matrix featureVector, Matrix normalVectors) {
    for (int i = 0; i < normalVectors.getColumnDimension(); i++) {
      Matrix n_i = normalVectors.getColumn(i);
      n_i.normalizeCols();
      double distance = RANDOM.nextGaussian() * jitter_std;
      featureVector = n_i.times(distance).plus(featureVector);
    }
    return featureVector;

//    int index = RANDOM.nextInt(normalVectors.getColumnDimension());
//    Matrix normalVector = normalVectors.getColumn(index);
//    double distance = RANDOM.nextGaussian() * JITTER_STANDARD_DEVIATION;
//    return normalVector.times(distance).plus(featureVector);

//    for (int i = 0; i < featureVector.getRowDimension(); i++) {
//      double j = (RANDOM.nextDouble() * 2 - 1) * (MAX_JITTER_PCT * (MAX - MIN) / 100.0);
//      featureVector.set(i, 0, featureVector.get(i, 0) + j);
//    }
//    return featureVector;
  }

  private boolean inMinMax(Matrix featureVector) {
    for (int i = 0; i < featureVector.getRowDimension(); i++) {
      for (int j = 0; j < featureVector.getColumnDimension(); j++) {
        double value = featureVector.get(i, j);
        if (value < min[i]) return false;
        if (value > max[i]) return false;
      }
    }
    return true;
  }

  private void output(PrintStream outStream, List<DoubleVector> featureVectors, LinearEquationSystem dependency, double std) {
    outStream.println("########################################################");
    outStream.println("### " + MIN_P + " [" + Util.format(min, ",", NF) + "]");
    outStream.println("### " + MAX_P + " [" + Util.format(max, ",", NF) + "]");
    outStream.println("### " + NUMBER_P + " " + number);
    outStream.println("### " + POINT_P + " [" + Util.format(point.getColumnPackedCopy(), NF) + "]");
    outStream.print("### " + BASIS_P + " ");
    for (int i = 0; i < basis.getColumnDimension(); i++) {
      outStream.print("[" + Util.format(basis.getColumn(i).getColumnPackedCopy(), NF) + "]");
      if (i < basis.getColumnDimension() - 1) outStream.print(",");
    }
    outStream.println();

    if (jitter != 0) {
      outStream.println("### max jitter in each dimension " + Util.format(jitter, NF) + "%");
      outStream.println("### Randomized standard deviation " + Util.format(jitter_std, NF));
      outStream.println("### Real       standard deviation " + Util.format(std, NF));
      outStream.println("###");
    }

    if (dependency != null) {
      outStream.println("### ");
      outStream.print("### dependency ");
      outStream.print(dependency.equationsToString("### ", NF.getMaximumFractionDigits()));
    }
    outStream.println("########################################################");

    for (DoubleVector featureVector : featureVectors) {
      if (label == null)
        outStream.println(featureVector);
      else {
        outStream.print(featureVector);
        outStream.println(" " + label);
      }
    }
  }

  private Matrix completeBasis(Matrix b) {
    StringBuffer msg = new StringBuffer();

    Matrix e = Matrix.unitMatrix(b.getRowDimension());
    Matrix basis = b.copy();
    Matrix result = null;
    for (int i = 0; i < e.getColumnDimension(); i++) {
      Matrix e_i = e.getColumn(i);
      boolean li = basis.linearlyIndependent(e_i);

      if (DEBUG) {
        msg.append("\nbasis ").append(basis.toString(NF));
        msg.append("\ne_i ").append(e_i.toString(NF));
        msg.append("\nlinearlyIndependent ").append(li);
        logger.fine(msg.toString());
      }

      if (li) {
        if (result == null) {
          result = e_i.copy();
        }
        else {
          result = appendColumn(result, e_i);
        }
        basis = appendColumn(basis, e_i);
      }
    }


    return result;
  }

  private Matrix appendColumn(Matrix m, Matrix column) {
    if (m.getRowDimension() != column.getRowDimension())
      throw new IllegalArgumentException("m.getRowDimension() != column.getRowDimension()");

    Matrix result = new Matrix(m.getRowDimension(), m.getColumnDimension() + column.getColumnDimension());
    for (int i = 0; i < result.getColumnDimension(); i++) {
      if (i < m.getColumnDimension()) {
        result.setColumn(i, m.getColumn(i));
      }
      else {
        result.setColumn(i, column.getColumn(i - m.getColumnDimension()));
      }
    }
    return result;
  }

  private Matrix orthonormalize(Matrix u) {
    Matrix v = u.getColumn(0).copy();

    for (int i = 1; i < u.getColumnDimension(); i++) {
      Matrix u_i = u.getColumn(i);
      Matrix sum = new Matrix(u.getRowDimension(), 1);
      for (int j = 0; j < i; j++) {
        Matrix v_j = v.getColumn(j);
        double scalar = u_i.scalarProduct(0, v_j, 0) / v_j.scalarProduct(0, v_j, 0);
        sum = sum.plus(v_j.times(scalar));
      }
      Matrix v_i = u_i.minus(sum);
      v = appendColumn(v, v_i);
    }

    v.normalizeCols();
    return v;
  }

  private double standardDeviation(List<DoubleVector> featureVectors, Matrix point, Matrix basis) {
    double std_2 = 0;
    for (DoubleVector doubleVector : featureVectors) {
      double distance = distance(doubleVector.getColumnVector(), point, basis);
      std_2 += distance * distance;
    }
    return Math.sqrt(std_2 / featureVectors.size());
  }

  private double distance(Matrix p, Matrix point, Matrix basis) {
    Matrix p_minus_a = p.minus(point);
    Matrix proj = p_minus_a.projection(basis);
    return p_minus_a.minus(proj).euclideanNorm(0);
  }

  /**
   * Generates noise and writes it to the specified outStream.
   *
   * @param outStream the output stram to write to
   */
  private void generateNoise(PrintStream outStream) {
    List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(number);
    int dim = min.length;
    for (int i = 0; i < number; i++) {
      double[] values = new double[dim];
      for (int d = 0; d < dim; d++) {
        values[d] = RANDOM.nextDouble() * (max[d] - min[d]) + min[d];
      }
      featureVectors.add(new DoubleVector(values));
    }
    output(outStream, featureVectors, null, 0);
  }

  /**
   * Returns the centroid of the feature space.
   *
   * @param dim the dimensionality of the feature space.
   * @return the centroid of the feature space
   */
  private Matrix centroid(int dim) {
    double[][] p = new double[dim][];
    for (int i = 0; i < p.length; i++) {
      p[i] = new double[]{(max[i] - min[i]) / 2};
    }
    return new Matrix(p);
  }

  /**
   * Returns a basis for for a hyperplane of the specified correleation dimension
   *
   * @param dim     the dimensionality of the feature space
   * @param corrDim the correlation dimensionality
   * @return a basis for a hyperplane of the specified correleation dimension
   */
  private Matrix correlationBasis(int dim, int corrDim) {
    double[][] b = new double[dim][corrDim];
    for (int i = 0; i < b.length; i++) {
      if (i < corrDim) {
        b[i][i] = 1;
      }
      else {
        for (int j = 0; j < corrDim; j++) {
          b[i][j] = RANDOM.nextDouble() * (max[i] - min[i]) + min[i];
        }
      }
    }
    Matrix basis = new Matrix(b);
    return basis;
  }

  private static class Dependency {
    Matrix basisVectors;
    Matrix normalVectors;
    LinearEquationSystem dependency;

    public Dependency(Matrix basisVectors, Matrix normalvectors, LinearEquationSystem linearEquationSystem) {
      this.basisVectors = basisVectors;
      this.normalVectors = normalvectors;
      this.dependency = linearEquationSystem;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      return
//      "basisVectors : " + basisVectors.toString(NF) +
//      "normalVectors: " + normalVectors.toString(NF) +
      "dependency: " + dependency.equationsToString(NF.getMaximumFractionDigits());
    }
  }
}
