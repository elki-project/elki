package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.VectorListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalListSizeConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalVectorListElementSizeConstraint;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides automatic generation of arbitrary oriented hyperplanes of arbitrary
 * correlation dimensionalities. <p/>
 *
 * @author Elke Achtert 
 */
public class ArbitraryCorrelationGenerator extends AxesParallelCorrelationGenerator {

  /**
   * Label for paremeter model point.
   */
  public static final String POINT_P = "point";

  /**
   * Description for parameter point.
   */
  public static final String POINT_D = "<p_1,...,p_d>a comma separated list of " +
                                       "the coordinates of the model point, " +
                                       "default is the centroid of the defined feature space.";

  /**
   * Label for parameter basis.
   */
  public static final String BASIS_P = "basis";

  /**
   * Description for parameter basis.
   */
  public static final String BASIS_D = "<b_11,...,b_1d:...:b_c1,...,b_cd>a list of basis vectors of the correlation hyperplane, "
                                       + "where c denotes the correlation dimensionality and d the dimensionality of the "
                                       + "feature space. Each basis vector is separated by :, the coordinates within "
                                       + "the basis vectors are separated by a comma. If no basis is specified, the basis vectors are generated randomly.";

  /**
   * Label for flag for gaussian distribution.
   */
  public static final String GAUSSIAN_F = "gaussian";

  /**
   * Description for flag gaussian.
   */
  public static final String GAUSSIAN_D = "flag to indicate gaussian distribution, default is an equal distribution.";

  /**
   * Parameter point.
   */
  private DoubleListParameter pointParameter;

  /**
   * The model point.
   */
  private Vector point;

  /**
   * Parameter basis.
   */
  private VectorListParameter basisParameter;

  /**
   * The basis vectors.
   */
  private Matrix basis;

  /**
   * The standard deviation for jitter.
   */
  private double jitter_std;

  /**
   * Indicates, if a gaussian distribution is desired.
   */
  private boolean gaussianDistribution;

  /**
   * Creates a new arbitrary correlation generator that provides automatic
   * generation of arbitrary oriented hyperplanes of arbitrary correlation
   * dimensionalities.
   */
  public ArbitraryCorrelationGenerator() {
    super();
    // parameter point
    pointParameter = new DoubleListParameter(POINT_P, POINT_D);
    pointParameter.setOptional(true);
    optionHandler.put(POINT_P, pointParameter);
    // global constraint
    try {
      GlobalParameterConstraint gpc = new GlobalListSizeConstraint(pointParameter,
                                                                   (IntParameter) optionHandler.getOption(DIM_P));
      optionHandler.setGlobalParameterConstraint(gpc);
    }
    catch (UnusedParameterException e) {
      this.verbose("Could not instantiate global parameter constraint: " + e.getMessage());
    }

    // parameter basis vectors
    VectorListParameter basis = new VectorListParameter(BASIS_P, BASIS_D);
    basis.setOptional(true);
    optionHandler.put(BASIS_P, basis);
    // global constraints
    try {
      GlobalParameterConstraint gpc = new GlobalListSizeConstraint(basis, (IntParameter) optionHandler.getOption(CORRDIM_P));
      optionHandler.setGlobalParameterConstraint(gpc);

      gpc = new GlobalVectorListElementSizeConstraint(basis, (IntParameter) optionHandler.getOption(DIM_P));
      optionHandler.setGlobalParameterConstraint(gpc);
    }
    catch (UnusedParameterException e) {
      verbose("Could not instantiate global parameter constraint: " + e.getMessage());
    }

    optionHandler.put(GAUSSIAN_F, new Flag(GAUSSIAN_F, GAUSSIAN_D));

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
      List<Double> pointList = (List<Double>) optionHandler.getOptionValue(POINT_P);
      double[] p = new double[dataDim];
      for (int i = 0; i < dataDim; i++) {

        p[i] = pointList.get(i);
      }
      point = new Vector(p);

    }
    else {
      point = centroid(dataDim);
    }

    // basis
    if (optionHandler.isSet(BASIS_P)) {
      List<List<Double>> basis_lists = (List<List<Double>>) optionHandler.getOptionValue(BASIS_P);

      double[][] b = new double[dataDim][corrDim];
      for (int c = 0; c < corrDim; c++) {
        List<Double> basisCoordinates = basis_lists.get(c);
        for (int d = 0; d < dataDim; d++) {

          b[d][c] = basisCoordinates.get(d);
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

    // gaussian distribution
    gaussianDistribution = optionHandler.isSet(GAUSSIAN_F);

    return remainingParameters;
  }

  /**
   * Generates a correlation hyperplane and writes it to the specified
   * according output stream writer.
   *
   * @param outStream the output stream to write into
   */
  void generateCorrelation(OutputStreamWriter outStream) throws IOException {
    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nbasis");
      msg.append(basis.toString(Format.NF4));
      msg.append("\npoint");
      msg.append(point.toString(Format.NF4));
      debugFine(msg.toString());
    }

    if (point.getRowDimensionality() != basis.getRowDimensionality())
      throw new IllegalArgumentException("point.getRowDimension() != basis.getRowDimension()!");

    if (point.getColumnDimensionality() != 1)
      throw new IllegalArgumentException("point.getColumnDimension() != 1!");

    if (!inMinMax(point))
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

    // adapt to min/max
    // FALSCH!!!
    // try {
    // Normalization n = new AttributeWiseRealVectorNormalization();
    // noinspection unchecked
    // List<DoubleVector> normalizedFeatureVectors =
    // n.normalize(featureVectors);
    // List<DoubleVector> denormalizedFeatureVectors = new
    // ArrayList<DoubleVector>(normalizedFeatureVectors.size());
    //
    // for (DoubleVector fv : normalizedFeatureVectors) {
    // double[] values = new double[fv.getDimensionality()];
    // for (int d = 1; d <= fv.getDimensionality(); d++) {
    // values[d - 1] = fv.getValue(d) * (max[d - 1] - min[d - 1]) + min[d -
    // 1];
    // }
    // denormalizedFeatureVectors.add(new DoubleVector(values));
    // }
    //
    // output(outStream, denormalizedFeatureVectors, dependency.dependency,
    // std);
    // }
    // catch (NonNumericFeaturesException e) {
    // e.printStackTrace();
    // }
  }

  /**
   * Determines the linear equation system describing the dependencies of the
   * correlation hyperplane depicted by the model point and the basis.
   *
   * @return the dependencies
   */
  private Dependency determineDependency() {
    StringBuffer msg = new StringBuffer();

    // orthonormal basis of subvectorspace U
    Matrix orthonormalBasis_U = basis.orthonormalize();
    Matrix completeVectors = orthonormalBasis_U.completeBasis();
    if (this.debug) {
      msg.append("\npoint ").append(point.toString(Format.NF4));
      msg.append("\nbasis ").append(basis.toString(Format.NF4));
      msg.append("\northonormal basis ").append(orthonormalBasis_U.toString(Format.NF4));
      msg.append("\ncomplete vectors ").append(completeVectors.toString(Format.NF4));
      debugFine(msg.toString());
    }

    // orthonormal basis of vectorspace V
    Matrix basis_V = orthonormalBasis_U.appendColumns(completeVectors);
    basis_V = basis_V.orthonormalize();
    if (this.debug) {
      debugFine("basis V " + basis_V.toString(Format.NF4));
    }

    // normal vectors of U
    Matrix normalVectors_U = basis_V.getMatrix(0, basis_V.getRowDimensionality() - 1, basis.getColumnDimensionality(), basis
        .getRowDimensionality()
                                                                                                                       - basis.getColumnDimensionality() + basis.getColumnDimensionality() - 1);
    if (this.debug) {
      debugFine("normal vector U " + normalVectors_U.toString(Format.NF4));
    }
    Matrix transposedNormalVectors = normalVectors_U.transpose();
    if (this.debug) {
      debugFine("tNV " + transposedNormalVectors.toString(Format.NF4));
      debugFine("point " + point.toString(Format.NF4));
    }

    // gauss jordan
    Matrix B = transposedNormalVectors.times(point);
    if (this.debug) {
      debugFine("B " + B.toString(Format.NF4));
    }
    Matrix gaussJordan = new Matrix(transposedNormalVectors.getRowDimensionality(), transposedNormalVectors.getColumnDimensionality()
                                                                                    + B.getColumnDimensionality());
    gaussJordan.setMatrix(0, transposedNormalVectors.getRowDimensionality() - 1, 0,
                          transposedNormalVectors.getColumnDimensionality() - 1, transposedNormalVectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimensionality() - 1, transposedNormalVectors.getColumnDimensionality(), gaussJordan
        .getColumnDimensionality() - 1, B);

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
   * Generates a vector lying on the hyperplane defined by the specified
   * parameters.
   *
   * @param point the model point of the hyperplane
   * @param basis the basis of the hyperplane
   * @return a matrix consisting of the po
   */
  private Vector generateFeatureVector(Vector point, Matrix basis) {
    Vector featureVector = point.copy();

    for (int i = 0; i < basis.getColumnDimensionality(); i++) {
      Vector b_i = basis.getColumnVector(i);

      double lambda_i;
      if (gaussianDistribution) {
        lambda_i = RANDOM.nextGaussian();
      }
      else {
        lambda_i = RANDOM.nextDouble();
        if (RANDOM.nextBoolean())
        {
          lambda_i *= -1;
        }
      }

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
    if (gaussianDistribution) {
      for (int i = 0; i < normalVectors.getColumnDimensionality(); i++) {
        Vector n_i = normalVectors.getColumnVector(i);
        n_i.normalizeColumns();
        double distance = RANDOM.nextGaussian() * jitter_std;
        featureVector = n_i.times(distance).plus(featureVector);
      }
    }
    else {
      double maxDist = Math.sqrt(dataDim);
      for (int i = 0; i < normalVectors.getColumnDimensionality(); i++) {
        Vector n_i = normalVectors.getColumnVector(i);
        n_i.normalizeColumns();
        double distance = RANDOM.nextDouble() * jitter * maxDist;
        featureVector = n_i.times(distance).plus(featureVector);

        // double v = featureVector.get(d);
        // if (RANDOM.nextBoolean())
        // featureVector.set(d, v + v * RANDOM.nextDouble() * jitter);
        // else
        // featureVector.set(d, v - v * RANDOM.nextDouble() * jitter);
      }
    }
    return featureVector;
  }

  /**
   * Returns true, if the specified feature vector is in the interval [min,
   * max], false otherwise.
   *
   * @param featureVector the feature vector to be tested
   * @return true, if the specified feature vector is in the interval [min,
   *         max], false otherwise
   */
  private boolean inMinMax(Vector featureVector) {
    for (int i = 0; i < featureVector.getRowDimensionality(); i++) {
      for (int j = 0; j < featureVector.getColumnDimensionality(); j++) {
        double value = featureVector.get(i, j);
        if (value < min[i])
        {
          return false;
        }
        if (value > max[i])
        {
          return false;
        }
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
  private void output(OutputStreamWriter outStream, List<DoubleVector> featureVectors, LinearEquationSystem dependency, double std)
      throws IOException {
    outStream.write("########################################################" + LINE_SEPARATOR);
    outStream.write("### " + MIN_P + " [" + Util.format(min, ",", Format.NF4) + "]" + LINE_SEPARATOR);
    outStream.write("### " + MAX_P + " [" + Util.format(max, ",", Format.NF4) + "]" + LINE_SEPARATOR);
    outStream.write("### " + NUMBER_P + " " + number + LINE_SEPARATOR);
    outStream.write("### " + POINT_P + " [" + Util.format(point.getColumnPackedCopy(), Format.NF4) + "]" + LINE_SEPARATOR);
    outStream.write("### " + BASIS_P + " ");
    for (int i = 0; i < basis.getColumnDimensionality(); i++) {
      outStream.write("[" + Util.format(basis.getColumn(i).getColumnPackedCopy(), Format.NF4) + "]");
      if (i < basis.getColumnDimensionality() - 1)
      {
        outStream.write(",");
      }
    }
    outStream.write(LINE_SEPARATOR);

    if (jitter != 0) {
      outStream.write("### max jitter in each dimension " + Util.format(jitter, Format.NF4) + "%" + LINE_SEPARATOR);
      outStream.write("### Randomized standard deviation " + Util.format(jitter_std, Format.NF4) + LINE_SEPARATOR);
      outStream.write("### Real       standard deviation " + Util.format(std, Format.NF4) + LINE_SEPARATOR);
      outStream.write("###" + LINE_SEPARATOR);
    }

    if (dependency != null) {
      outStream.write("### " + LINE_SEPARATOR);
      outStream.write("### dependency ");
      outStream.write(dependency.equationsToString("### ", Format.NF4.getMaximumFractionDigits()));
    }
    outStream.write("########################################################" + LINE_SEPARATOR);

    for (DoubleVector featureVector : featureVectors) {
      if (label == null)
      {
        outStream.write(featureVector + LINE_SEPARATOR);
      }
      else {
        outStream.write(featureVector.toString());
        outStream.write(" " + label + LINE_SEPARATOR);
      }
    }
  }

  /**
   * Returns the standard devitaion of the distance of the feature vectors to
   * the hyperplane defined by the specified point and basis.
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
   * Returns the distance of the specified feature vector to the hyperplane
   * defined by the specified point and basis.
   *
   * @param featureVector the feature vector
   * @param point         the model point of the hyperplane
   * @param basis         the basis of the hyperplane
   * @return the distance of the specified feature vector to the hyperplane
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
   * Returns a basis for for a hyperplane of the specified correleation
   * dimension
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
          // "basisVectors : " + basisVectors.toString(NF) +
          // "normalVectors: " + normalVectors.toString(NF) +
          "dependency: " + dependency.equationsToString(Format.NF4.getMaximumFractionDigits());
    }
  }
}