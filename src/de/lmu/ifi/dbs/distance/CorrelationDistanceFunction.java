package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract class that provides the Correlation distance for real valued vectors.
 * All subclasses must implement a method to process the preprocessing step
 * in terms of doing the PCA for each object of the database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CorrelationDistanceFunction extends RealVectorDistanceFunction {

  /**
   * The association id to associate a pca to an object.
   */
  public static final String ASSOCIATION_ID_PCA = CorrelationDimensionPreprocessor.ASSOCIATION_ID_PCA;

  /**
   * Indicates a separator.
   */
  public static final Pattern SEPARATOR = Pattern.compile("x");

  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "<double>a double specifying the threshold of a " +
                                       "distance between a vector q and a given space " +
                                       "that indicates that q adds a new dimension " +
                                       "to the space (default is delta = " + DEFAULT_DELTA + ")";

  /**
   * The default preprocessor class name.
   */
  public static final Class DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedCorrelationDimensionPreprocessor.class;

  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_P = "preprocessor";


  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_CLASS_D = "<classname>the preprocessor to determine the correlation dimensions " +
                                                    "of the objects - must implement " +
                                                    CorrelationDimensionPreprocessor.class.getName() + ". " +
                                                    "(Default: " + DEFAULT_PREPROCESSOR_CLASS.getName() + ").";

  /**
   * OptionHandler for handling options.
   */
  protected OptionHandler optionHandler;

  /**
   * The database that holds the associations for the MetricalObject
   * for which the distances should be computed.
   */
  protected Database db;

  /**
   * The threshold of a distance between a vector q and a given space
   * that indicates that q adds a new dimension to the space. 
   */
  private double delta;

  /**
   * The preprocessor to determine the correlation dimensions of the objects.
   */
  private CorrelationDimensionPreprocessor preprocessor;

  /**
   * Provides a CorrelationDistanceFunction with a pattern defined to accept
   * Strings that define an Integer followed by a separator followed by a Double.
   */
  public CorrelationDistanceFunction() {
    super(Pattern.compile("\\d+" + SEPARATOR.pattern() + "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));

    Map<String, String> parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(DELTA_P + OptionHandler.EXPECTS_VALUE, DELTA_D);
    parameterToDescription.put(PREPROCESSOR_CLASS_P + OptionHandler.EXPECTS_VALUE, PREPROCESSOR_CLASS_D);
    optionHandler = new OptionHandler(parameterToDescription, "");
  }

  /**
   * Provides the Correlation distance between the given two vectors.
   *
   * @return the Correlation distance between the given two vectors as an
   *         instance of {@link CorrelationDistance CorrelationDistance}.
   * @see RealVectorDistanceFunction#distance(de.lmu.ifi.dbs.data.RealVector,
   *      de.lmu.ifi.dbs.data.RealVector)
   */
  public Distance distance(RealVector rv1, RealVector rv2) {
    return correlationDistance(rv1, rv2);
  }

  /**
   * Provides a distance suitable to this DistanceFunction
   * based on the given pattern.
   *
   * @param pattern A pattern defining a distance suitable to this DistanceFunction
   * @return a distance suitable to this DistanceFunction
   *         based on the given pattern
   * @throws IllegalArgumentException if the given pattern is not compatible
   *                                  with the requirements of this DistanceFunction
   */
  public Distance valueOf(String pattern) throws IllegalArgumentException {
    if (matches(pattern)) {
      String[] values = SEPARATOR.split(pattern);
      return new CorrelationDistance(Integer.parseInt(values[0]),
                                     Double.parseDouble(values[1]));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern +
                                         "\" does not match required pattern \"" +
                                         requiredInputPattern() + "\"");
    }
  }

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  public Distance infiniteDistance() {
    return new CorrelationDistance(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  public Distance nullDistance() {
    return new CorrelationDistance(0, 0);
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  public Distance undefinedDistance() {
    return new CorrelationDistance(-1, Double.NaN);
  }

  /**
   * Returns a description of the class and the required parameters.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    return "Correlation distance for RealVectors. No parameters required. " +
           "Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }

  /**
   * Computes the necessary PCA associations for
   * each object of the database.
   * Afterwards the database is set to get later on
   * the PCA associations needed for distance computing.
   *
   * @param db the database to be set
   */
  public void setDatabase(Database db) {
    this.db = db;
    preprocessor.run(db);
  }

  /**
   * Sets the values for the parameters delta and preprocessor if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if (optionHandler.isSet(DELTA_P)) {
      try {
        delta = Double.parseDouble(optionHandler.getOptionValue(DELTA_P));
        if (delta < 0)
          throw new IllegalArgumentException("CorrelationDistanceFunction: delta has to be greater than zero!");
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      delta = DEFAULT_DELTA;
    }

    if (optionHandler.isSet(PREPROCESSOR_CLASS_P)) {
      try {
        preprocessor = (CorrelationDimensionPreprocessor) Class.forName(optionHandler.getOptionValue(PREPROCESSOR_CLASS_P)).newInstance();
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      try {
        preprocessor = (CorrelationDimensionPreprocessor) DEFAULT_PREPROCESSOR_CLASS.newInstance();
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    return preprocessor.setParameters(remainingParameters);
  }

  /**
   * Computes the correlation distance between the two specified vectors.
   *
   * @param rv1 first RealVector
   * @param rv2 second RealVector
   * @return the correlation distance between the two specified vectors
   */
  private CorrelationDistance correlationDistance(RealVector rv1, RealVector rv2) {
    // TODO nur in eine Richtung?
    int dim = rv1.getDimensionality();

    // pca of rv1
    CorrelationPCA pca1 = (CorrelationPCA) db.getAssociation(ASSOCIATION_ID_PCA, rv1.getID());
    Matrix v1 = pca1.getEigenvectors();
    Matrix v1_strong = pca1.strongEigenVectors();
    Matrix e1_czech = pca1.getSelectionMatrixOfStrongEigenvectors().copy();
    int lambda1 = pca1.getCorrelationDimension();
//    int lambda1 = 0;

    // pca of rv2
    CorrelationPCA pca2 = (CorrelationPCA) db.getAssociation(ASSOCIATION_ID_PCA, rv2.getID());
    Matrix v2 = pca2.getEigenvectors();
    Matrix v2_strong = pca2.strongEigenVectors();
    Matrix e2_czech = pca2.getSelectionMatrixOfStrongEigenvectors();
    int lambda2 = pca2.getCorrelationDimension();
//    int lambda2 = 0;

    // for all strong eigenvectors of rv2
    Matrix m1_czech = v1.times(e1_czech).times(v1.transpose());
    for (int i = 0; i < v2_strong.getColumnDimension(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of rv1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transpose().times(v2_i).get(0, 0) -
                              v2_i.transpose().times(m1_czech).times(v2_i).get(0, 0));

      // if so, insert v2_i into v1 and adjust v1
      // and compute m1_czech new, increase lambda1
      if (lambda1 < dim && dist > delta) {
        adjust(v1, e1_czech, v2_i, lambda1++);
        m1_czech = v1.times(e1_czech).times(v1.transpose());
      }
    }

    // for all strong eigenvectors of rv1
    Matrix m2_czech = v2.times(e2_czech).times(v2.transpose());
    for (int i = 0; i < v1_strong.getColumnDimension(); i++) {
      Matrix v1_i = v1_strong.getColumn(i);
      // check, if distance of v1_i to the space of rv2 > delta
      // (i.e., if v1_i spans up a new dimension)
      double dist = Math.sqrt(v1_i.transpose().times(v1_i).get(0, 0) -
                              v1_i.transpose().times(m2_czech).times(v1_i).get(0, 0));

      // if so, insert v1_i into v2 and adjust v2
      // and compute m2_czech new , increase lambda2
      if (lambda2 < dim && dist > delta) {
        adjust(v2, e2_czech, v1_i, lambda2++);
        m2_czech = v2.times(e2_czech).times(v2.transpose());
      }
    }

    int correlationDistance = Math.max(lambda1, lambda2);

//    TODO
//    Matrix m_1_czech = v1.times(e1_czech).times(v1.transpose());
//    double dist_1 = normalizedDistance(rv1, rv2, m1_czech);
//    Matrix m_2_czech = v2.times(e2_czech).times(v2.transpose());
//    double dist_2 = normalizedDistance(rv1, rv2, m2_czech);
//    if (dist_1 > delta || dist_2 > delta) {
//      correlationDistance++;
//    }

    double euclideanDistance = euclideanDistance(rv1, rv2);
    return new CorrelationDistance(correlationDistance, euclideanDistance);
  }

  /**
   * Inserts the specified vector into the given orthonormal matrix <code>v</code> at
   * column <code>corrDim</code>. After insertion the matrix <code>v</code>
   * is orthonormalized and column <code>corrDim</code> of matrix
   * <code>e_czech</code> is set to the <code>corrDim</code>-th unit vector..
   *
   * @param v       the orthonormal matrix of the eigenvectors
   * @param e_czech the selection matrix of the strong eigenvectors
   * @param vector  the vector to be inserted
   * @param corrDim the column at which the vector should be inserted
   */
  private void adjust(Matrix v, Matrix e_czech, Matrix vector, int corrDim) {
    int dim = v.getRowDimension();

    // set e_czech[corrDim][corrDim] := 1
    e_czech.set(corrDim, corrDim, 1);

    // normalize v
    Matrix v_i = vector.copy();
    Matrix sum = new Matrix(dim, 1);
    for (int k = 0; k < corrDim; k++) {
      Matrix v_k = v.getColumn(k);
      sum = sum.plus(v_k.times(v_i.scalarProduct(0, v_k, 0)));
    }
    v_i = v_i.minus(sum);
    v_i = v_i.times(1.0 / v_i.euclideanNorm(0));
    v.setColumn(corrDim, v_i);
  }

  /**
   *
   * @return the Euklidean distance between the given two vectors
   */

  /**
   * Computes the euklidean distance between the given two vectors.
   *
   * @param rv1 first RealVector
   * @param rv2 second RealVector
   * @return the euklidean distance between the given two vectors
   */
  private double euclideanDistance(RealVector rv1, RealVector rv2) {
    if (rv1.getDimensionality() != rv2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of RealVectors\n  first argument: " + rv1.toString() + "\n  second argument: " + rv2.toString());
    }

    double sqrDist = 0;
    for (int i = 1; i <= rv1.getDimensionality(); i++) {
      double manhattanI = rv1.getValue(i) - rv2.getValue(i);
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

}
