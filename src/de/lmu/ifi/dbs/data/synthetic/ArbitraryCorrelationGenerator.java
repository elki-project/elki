package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides automatic generation of arbitrary oriented hyperplanes of arbitrary correlation dimensionalities.
 * <p/>
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ArbitraryCorrelationGenerator extends AxesParallelCorrelationGenerator {

  /**
   * Parameter for model point.
   */
  public static final String POINT_P = "point";

  /**
   * Description for parameter point.
   */
  public static final String POINT_D = "<p_1,...,p_d>a comma separated list of the coordinates of the model point, " +
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
                                       "the basis vectors are separated by a comma. If no basis is specified, the " +
                                       "basis vectors are generated randomly.";

  /**
   * Number Formatter for output.
   */
  private static final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * The model point.
   */
  private Vector point;

  /**
   * The basis vectors.
   */
  private Matrix basis;

  /**
   * The standard deviation for jitter.
   */
  private double jitter_std;

  /**
   * Creates a new arbitrary correlation generator that provides automatic generation
   * of arbitrary oriented hyperplanes of arbitrary correlation dimensionalities.
   */
  public ArbitraryCorrelationGenerator() {
    super();
    optionHandler.put(POINT_P, new Parameter(POINT_P, POINT_D));
    optionHandler.put(BASIS_P, new Parameter(BASIS_P, BASIS_D));
  }

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
    ArbitraryCorrelationGenerator wrapper = new ArbitraryCorrelationGenerator();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (Exception e) {
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
      e.printStackTrace();
    }
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // model point
    if (optionHandler.isSet(POINT_P)) {
      String pointString = optionHandler.getOptionValue(POINT_P);
      String[] pointCoordinates = COMMA_SPLIT.split(pointString);
      if (pointCoordinates.length != dataDim)
        throw new WrongParameterValueException("Value of parameter " + POINT_P + " has not the specified dimensionality  " + DIM_P + " = " + dataDim);

      double[] p = new double[dataDim];
      for (int i = 0; i < dataDim; i++) {
        try {
          p[i] = Double.parseDouble(pointCoordinates[i]);
        }
        catch (NumberFormatException e) {
          throw new WrongParameterValueException(POINT_P, pointString, POINT_D);
        }
      }
      point = new Vector(p);
    }
    else {
      point = centroid(dataDim);
    }

    // basis
    if (optionHandler.isSet(BASIS_P)) {
      String basisString = optionHandler.getOptionValue(BASIS_P);
      String[] basisVectors = VECTOR_SPLIT.split(basisString);
      if (basisVectors.length != corrDim) {
        throw new WrongParameterValueException("Value of parameter " + BASIS_P + " has not the specified dimensionality  " +
                                               CORRDIM_P + " = " + corrDim);
      }

      double[][] b = new double[dataDim][corrDim];
      for (int c = 0; c < corrDim; c++) {
        String[] basisCoordinates = COMMA_SPLIT.split(basisVectors[c]);
        if (basisCoordinates.length != dataDim)
          throw new WrongParameterValueException("Value of parameter " + BASIS_P + " has not the specified dimensionality  " + DIM_P + " = " + dataDim);

        for (int d = 0; d < dataDim; d++) {
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
      basis = correlationBasis(dataDim, corrDim);
    }

    // jitter std
    jitter_std = 0;
    for (int d = 0; d < dataDim; d++) {
      jitter_std = Math.max(jitter * (max[d] - min[d]), jitter_std);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(POINT_P, Util.format(point.getArray()[0]));
    mySettings.addSetting(BASIS_P, Util.format(basis.getArray(), ":", ",", 2));
    return settings;
  }

  /**
   * Generates a correlation hyperplane and writes
   * it to the specified according output stream writer.
   *
   * @param outStream the output stream to write into
   */
  void generateCorrelation(OutputStreamWriter outStream) throws IOException {
    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nbasis");
      msg.append(basis.toString(NF));
      msg.append("\npoint");
      msg.append(point.toString(NF));
      debugFine(msg.toString());
    }

    if (point.getRowDimensionality() != basis.getRowDimensionality())
      throw new IllegalArgumentException("point.getRowDimension() != basis.getRowDimension()!");

    if (point.getColumnDimensionality() != 1)
      throw new IllegalArgumentException("point.getColumnDimension() != 1!");

    if (! inMinMax(point))
      throw new IllegalArgumentException("point not in min max!");

    if (basis.getColumnDimensionality() == basis.getRowDimensionality()) {
      generateNoise(outStream);
      return;
    }

    // determine the dependency
    Dependency dependency = determineDependency();
    if (isVerbose()) {
      StringBuffer msg = new StringBuffer();
      msg.append(dependency.toString());
      verbose(msg.toString());
    }

    Matrix b = dependency.basisVectors;

    List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(number);
    while (featureVectors.size() != number) {
      Vector featureVector = generateFeatureVector(point, b);
      double distance = distance(featureVector, point, b);
      if (distance > 1E-13 && isVerbose())
        verbose("distance " + distance);
      if (jitter != 0) {
        featureVector = jitter(featureVector, dependency.normalVectors);
      }
      if (inMinMax(featureVector)) {
        featureVectors.add(new DoubleVector(featureVector));
      }
    }

    double std = standardDeviation(featureVectors, point, b);
    if (isVerbose()) {
      verbose("standard deviation " + std);
    }
    output(outStream, featureVectors, dependency.dependency, std);
  }

  /**
   * Determines the linear equation system describing the dependencies
   * of the correlation hyperplane depicted by the model point and the basis.
   *
   * @return the dependencies
   */
  private Dependency determineDependency() {
    StringBuffer msg = new StringBuffer();

    // orthonormal basis of subvectorspace U
    Matrix orthonormalBasis_U = orthonormalize(basis);
    Matrix completeVectors = completeBasis(orthonormalBasis_U);
    if (this.debug) {
      msg.append("\npoint ").append(point.toString(NF));
      msg.append("\nbasis ").append(basis.toString(NF));
      msg.append("\northonormal basis ").append(orthonormalBasis_U.toString(NF));
      msg.append("\ncomplete vectors ").append(completeVectors.toString(NF));
      debugFine(msg.toString());
    }

    // orthonormal basis of vectorspace V
    Matrix basis_V = appendColumns(orthonormalBasis_U, completeVectors);
    basis_V = orthonormalize(basis_V);
    if (this.debug) {
      debugFine("basis V " + basis_V.toString(NF));
    }

    // normal vectors of U
    Matrix normalVectors_U = basis_V.getMatrix(0, basis_V.getRowDimensionality() - 1,
                                               basis.getColumnDimensionality(),
                                               basis.getRowDimensionality() - basis.getColumnDimensionality() + basis.getColumnDimensionality() - 1);
    if (this.debug) {
      debugFine("normal vector U " + normalVectors_U.toString(NF));
    }
    Matrix transposedNormalVectors = normalVectors_U.transpose();
    if (this.debug) {
      debugFine("tNV " + transposedNormalVectors.toString(NF));
      debugFine("point " + point.toString(NF));
    }

    // gauss jordan
    Matrix B = transposedNormalVectors.times(point);
    if (this.debug) {
      debugFine("B " + B.toString(NF));
    }
    Matrix gaussJordan = new Matrix(transposedNormalVectors.getRowDimensionality(), transposedNormalVectors.getColumnDimensionality() + B.getColumnDimensionality());
    gaussJordan.setMatrix(0, transposedNormalVectors.getRowDimensionality() - 1, 0, transposedNormalVectors.getColumnDimensionality() - 1, transposedNormalVectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimensionality() - 1, transposedNormalVectors.getColumnDimensionality(), gaussJordan.getColumnDimensionality() - 1, B);

    double[][] a = new double[transposedNormalVectors.getRowDimensionality()][transposedNormalVectors.getColumnDimensionality()];
    double[][] we = transposedNormalVectors.getArray();
    double[] b = B.getColumn(0).getRowPackedCopy();
    System.arraycopy(we, 0, a, 0, transposedNormalVectors.getRowDimensionality());

    LinearEquationSystem lq = new LinearEquationSystem(a, b);
    lq.solveByTotalPivotSearch();
    Dependency dependency = new Dependency(orthonormalBasis_U, normalVectors_U, lq);

    return dependency;
  }

  /**
   * Generates a vector lying on the hyperplane defined by the
   * specified parameters.
   *
   * @param point the model point of the hyperplane
   * @param basis the basis  of the hyperplane
   * @return a matrix consisting of the po
   */
  private Vector generateFeatureVector(Vector point, Matrix basis) {
    Vector featureVector = point.copy();
    for (int i = 0; i < basis.getColumnDimensionality(); i++) {
      double lambda_i = RANDOM.nextGaussian();
      Vector b_i = basis.getColumnVector(i);
      featureVector = featureVector.plus(b_i.times(lambda_i));
    }
    return featureVector;
  }

  /**
   * Adds a jitter to each dimension of the specified feature vector.
   *
   * @param featureVector the feature vector
   * @param normalVectors the normal vectors
   * @return the new (jittered) feature vector
   */
  private Vector jitter(Vector featureVector, Matrix normalVectors) {
    for (int i = 0; i < normalVectors.getColumnDimensionality(); i++) {
      Vector n_i = normalVectors.getColumnVector(i);
      n_i.normalizeColumns();
      double distance = RANDOM.nextGaussian() * jitter_std;
      featureVector = n_i.times(distance).plus(featureVector);
    }
    return featureVector;
  }

  /**
   * Returns true, if the specified feature vector is in
   * the interval [min, max], false otherwise.
   *
   * @param featureVector the feature vector to be tested
   * @return true, if the specified feature vector is in
   *         the interval [min, max], false otherwise
   */
  private boolean inMinMax(Vector featureVector) {
    for (int i = 0; i < featureVector.getRowDimensionality(); i++) {
      for (int j = 0; j < featureVector.getColumnDimensionality(); j++) {
        double value = featureVector.get(i, j);
        if (value < min[i]) return false;
        if (value > max[i]) return false;
      }
    }
    return true;
  }

  /**
   * Writes the specified list of feature vectors to the output.
   *
   * @param outStream      the output stream to write into
   * @param featureVectors the feature vectors to be written
   * @param dependency     the dependeny of the feature vectors
   * @param std            the standard deviation of the jitter of the feature vectors
   * @throws IOException
   */
  private void output(OutputStreamWriter outStream, List<DoubleVector> featureVectors, LinearEquationSystem dependency, double std) throws IOException {
    outStream.write("########################################################" + LINE_SEPARATOR);
    outStream.write("### " + MIN_P + " [" + Util.format(min, ",", NF) + "]" + LINE_SEPARATOR);
    outStream.write("### " + MAX_P + " [" + Util.format(max, ",", NF) + "]" + LINE_SEPARATOR);
    outStream.write("### " + NUMBER_P + " " + number + LINE_SEPARATOR);
    outStream.write("### " + POINT_P + " [" + Util.format(point.getColumnPackedCopy(), NF) + "]" + LINE_SEPARATOR);
    outStream.write("### " + BASIS_P + " ");
    for (int i = 0; i < basis.getColumnDimensionality(); i++) {
      outStream.write("[" + Util.format(basis.getColumn(i).getColumnPackedCopy(), NF) + "]");
      if (i < basis.getColumnDimensionality() - 1) outStream.write(",");
    }
    outStream.write(LINE_SEPARATOR);

    if (jitter != 0) {
      outStream.write("### max jitter in each dimension " + Util.format(jitter, NF) + "%" + LINE_SEPARATOR);
      outStream.write("### Randomized standard deviation " + Util.format(jitter_std, NF) + LINE_SEPARATOR);
      outStream.write("### Real       standard deviation " + Util.format(std, NF) + LINE_SEPARATOR);
      outStream.write("###" + LINE_SEPARATOR);
    }

    if (dependency != null) {
      outStream.write("### " + LINE_SEPARATOR);
      outStream.write("### dependency ");
      outStream.write(dependency.equationsToString("### ", NF.getMaximumFractionDigits()));
    }
    outStream.write("########################################################" + LINE_SEPARATOR);

    for (DoubleVector featureVector : featureVectors) {
      if (label == null)
        outStream.write(featureVector + LINE_SEPARATOR);
      else {
        outStream.write(featureVector.toString());
        outStream.write(" " + label + LINE_SEPARATOR);
      }
    }
  }

  /**
   * Completes the specified d x c basis of a subspace of R^d to a
   * d x d basis of R^d, i.e. appends c-d columns to the specified basis b.
   *
   * @param b the basis of the subspace of R^d
   * @return a basis of R^d
   */
  private Matrix completeBasis(Matrix b) {
    StringBuffer msg = new StringBuffer();

    Matrix e = Matrix.unitMatrix(b.getRowDimensionality());
    Matrix basis = b.copy();
    Matrix result = null;
    for (int i = 0; i < e.getColumnDimensionality(); i++) {
      Matrix e_i = e.getColumn(i);
      boolean li = basis.linearlyIndependent(e_i);

      if (this.debug) {
        msg.append("\nbasis ").append(basis.toString(NF));
        msg.append("\ne_i ").append(e_i.toString(NF));
        msg.append("\nlinearlyIndependent ").append(li);
        debugFine(msg.toString());
      }

      if (li) {
        if (result == null) {
          result = e_i.copy();
        }
        else {
          result = appendColumns(result, e_i);
        }
        basis = appendColumns(basis, e_i);
      }
    }

    return result;
  }

  /**
   * Appends the specified columns to the given matrix.
   *
   * @param m       the matrix
   * @param columns the columns to be appended
   * @return the new matrix with the appended columns
   */
  private Matrix appendColumns(Matrix m, Matrix columns) {
    if (m.getRowDimensionality() != columns.getRowDimensionality())
      throw new IllegalArgumentException("m.getRowDimension() != column.getRowDimension()");

    Matrix result = new Matrix(m.getRowDimensionality(), m.getColumnDimensionality() + columns.getColumnDimensionality());
    for (int i = 0; i < result.getColumnDimensionality(); i++) {
      if (i < m.getColumnDimensionality()) {
        result.setColumn(i, m.getColumn(i));
      }
      else {
        result.setColumn(i, columns.getColumn(i - m.getColumnDimensionality()));
      }
    }
    return result;
  }

  /**
   * Orthonormalizes the specified matrix.
   *
   * @param u the matrix to be orthonormalized
   * @return the orthonormalized matrixr
   */
  private Matrix orthonormalize(Matrix u) {
    Matrix v = u.getColumn(0).copy();

    for (int i = 1; i < u.getColumnDimensionality(); i++) {
      Matrix u_i = u.getColumn(i);
      Matrix sum = new Matrix(u.getRowDimensionality(), 1);
      for (int j = 0; j < i; j++) {
        Matrix v_j = v.getColumn(j);
        double scalar = u_i.scalarProduct(0, v_j, 0) / v_j.scalarProduct(0, v_j, 0);
        sum = sum.plus(v_j.times(scalar));
      }
      Matrix v_i = u_i.minus(sum);
      v = appendColumns(v, v_i);
    }

    v.normalizeColumns();
    return v;
  }

  /**
   * Returns the standard devitaion of the distance of the feature vectors to the
   * hyperplane defined by the specified point and basis.
   *
   * @param featureVectors the feature vectors
   * @param point          the model point of the hyperplane
   * @param basis          the basis of the hyperplane
   * @return the standard devitaion of the distance
   */
  private double standardDeviation(List<DoubleVector> featureVectors, Vector point, Matrix basis) {
    double std_2 = 0;
    for (DoubleVector doubleVector : featureVectors) {
      double distance = distance(doubleVector.getColumnVector(), point, basis);
      std_2 += distance * distance;
    }
    return Math.sqrt(std_2 / featureVectors.size());
  }

  /**
   * Returns the distance of the specified feature vector to the
   * hyperplane defined by the specified point and basis.
   *
   * @param featureVector the feature vector
   * @param point         the model point of the hyperplane
   * @param basis         the basis of the hyperplane
   * @return the distance of the specified feature vector to the
   *         hyperplane
   */
  private double distance(Vector featureVector, Vector point, Matrix basis) {
    Matrix p_minus_a = featureVector.minus(point);
    Matrix proj = p_minus_a.projection(basis);
    return p_minus_a.minus(proj).euclideanNorm(0);
  }

  /**
   * Generates noise and writes it to the specified outStream.
   *
   * @param outStream the output stream to write to
   */
  private void generateNoise(OutputStreamWriter outStream) throws IOException {
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
  private Vector centroid(int dim) {
    double[] p = new double[dim];
    for (int i = 0; i < p.length; i++) {
      p[i] = (max[i] - min[i]) / 2;
    }
    return new Vector(p);
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

  /**
   * Encapsulates dependencies.
   */
  private static class Dependency {
    /**
     * The basis vectors.
     */
    Matrix basisVectors;

    /**
     * The normal vectors.
     */
    Matrix normalVectors;

    /**
     * The linear equation system.
     */
    LinearEquationSystem dependency;

    /**
     * Provied a new dependency object.
     *
     * @param basisVectors         the basis vectors
     * @param normalvectors        the normal vectors
     * @param linearEquationSystem the linear equation system
     */
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
