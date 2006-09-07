package de.lmu.ifi.dbs.utilities;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.logging.StaticLogger;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import sun.misc.Launcher;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @version 0.1
 */
public final class Util extends AbstractLoggable {

  /**
   * The logger of this class.
   */

  private static StaticLogger logger = new StaticLogger(Util.class.getName());

  static {
    if (LoggingConfiguration.isChangeable()) {
      LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
    }
  }

  public Util() {
    super(LoggingConfiguration.DEBUG);
  }

  /**
   * Returns the maximum of the given Distances or the first, if none is
   * greater than the other one.
   *
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the maximum of the given Distances or the first, if
   *         neither is greater than the other one
   */
  public static <D extends Distance<D>> D max(D d1, D d2) {
    if (d1.compareTo(d2) > 0) {
      return d1;
    }
    else if (d2.compareTo(d1) > 0) {
      return d2;
    }
    else {
      return d1;
    }
  }

  /**
   * Returns the minimum of the given Distances or the first, if none is less
   * than the other one.
   *
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the minimum of the given Distances or the first, if
   *         neither is less than the other one
   */
  public static <D extends Distance<D>> D min(D d1, D d2) {
    if (d1.compareTo(d2) < 0) {
      return d1;
    }
    else if (d2.compareTo(d1) < 0) {
      return d2;
    }
    else {
      return d1;
    }
  }

  /**
   * Formats the double d with 2 fraction digits.
   *
   * @param d the double to be formatted
   * @return a String representing the double d
   */
  public static String format(final double d) {
    return format(d, 2);
  }

  /**
   * Formats the double d with the specified fraction digits.
   *
   * @param d      the double array to be formatted
   * @param digits the number of fraction digits
   * @return a String representing the double d
   */
  public static String format(final double d, int digits) {
    final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(digits);
    nf.setGroupingUsed(false);
    return nf.format(d);
  }

  /**
   * Formats the double d with the specified number format.
   *
   * @param d  the double array to be formatted
   * @param nf the number format to be used for formatting
   * @return a String representing the double d
   */
  public static String format(final double d, NumberFormat nf) {
    return nf.format(d);
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   *
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[] d) {
    return format(d, ", ", 2);
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   *
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[][] d) {
    StringBuffer buffer = new StringBuffer();
    for (double[] array : d) {
      buffer.append(format(array, ", ", 2) + "\n");
    }
    return buffer.toString();
  }

  /**
   * Formats the array of double arrays d with 'the specified separators and fraction digits.
   *
   * @param d      the double array to be formatted
   * @param sep1   the first separator of the outer array
   * @param sep2   the second separator of the inner array
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[][] d, String sep1, String sep2, int digits) {
    StringBuffer buffer = new StringBuffer();

    for (int i = 0; i < d.length; i++) {
      if (i < d.length - 1) {
        buffer.append(format(d[i], sep2, digits)).append(sep1);
      }
      else {
        buffer.append(format(d[i], sep2, digits));
      }
    }

    return buffer.toString();
  }

  /**
   * Formats the double array d with the specified separator.
   *
   * @param d   the double array to be formatted
   * @param sep the seperator between the single values of the double array,
   *            e.g. ','
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < d.length; i++) {
      if (i < d.length - 1) {
        buffer.append(d[i]).append(sep);
      }
      else {
        buffer.append(d[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with the specified separator and the specified
   * fraction digits.
   *
   * @param d      the double array to be formatted
   * @param sep    the seperator between the single values of the double array,
   *               e.g. ','
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep, int digits) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < d.length; i++) {
      if (i < d.length - 1) {
        buffer.append(format(d[i], digits)).append(sep);
      }
      else {
        buffer.append(format(d[i], digits));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with the specified number format.
   *
   * @param d  the double array to be formatted
   * @param nf the number format to be used for formatting
   * @return a String representing the double array d
   */
  public static String format(double[] d, NumberFormat nf) {
    return format(d, " ", nf);
  }

  /**
   * Formats the double array d with the specified number format.
   *
   * @param d   the double array to be formatted
   * @param sep the seperator between the single values of the double array,
   *            e.g. ','
   * @param nf  the number format to be used for formatting
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep, NumberFormat nf) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < d.length; i++) {
      if (i < d.length - 1) {
        buffer.append(format(d[i], nf)).append(sep);
      }
      else {
        buffer.append(format(d[i], nf));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the float array f with the specified separator and the specified
   * fraction digits.
   *
   * @param f      the float array to be formatted
   * @param sep    the seperator between the single values of the float array,
   *               e.g. ','
   * @param digits the number of fraction digits
   * @return a String representing the float array f
   */
  public static String format(float[] f, String sep, int digits) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < f.length; i++) {
      if (i < f.length - 1) {
        buffer.append(format(f[i], digits)).append(sep);
      }
      else {
        buffer.append(format(f[i], digits));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the float array f with ',' as separator and 2 fraction digits.
   *
   * @param f the float array to be formatted
   * @return a String representing the float array f
   */
  public static String format(float[] f) {
    return format(f, ", ", 2);
  }

  /**
   * Formats the int array a for printing purposes.
   *
   * @param a   the int array to be formatted
   * @param sep the seperator between the single values of the float array,
   *            e.g. ','
   * @return a String representing the int array a
   */
  public static String format(int[] a, String sep) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < a.length; i++) {
      if (i < a.length - 1) {
        buffer.append(a[i]).append(sep);
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the int array a for printing purposes.
   *
   * @param a the int array to be formatted
   * @return a String representing the int array a
   */
  public static String format(int[] a) {
    return format(a, ", ");
  }

  /**
   * Formats the long array a for printing purposes.
   *
   * @param a the long array to be formatted
   * @return a String representing the long array a
   */
  public static String format(long[] a) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < a.length; i++) {
      if (i < a.length - 1) {
        buffer.append(a[i]).append(", ");
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the byte array a for printing purposes.
   *
   * @param a the byte array to be formatted
   * @return a String representing the byte array a
   */
  public static String format(byte[] a) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < a.length; i++) {
      if (i < a.length - 1) {
        buffer.append(a[i]).append(", ");
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the boolean b.
   *
   * @param b the boolean to be formatted
   * @return a String representing of the boolean b
   */
  public static String format(final boolean b) {
    if (b) {
      return "1";
    }
    return "0";
  }

  /**
   * Formats the boolean array b with ',' as separator.
   *
   * @param b   the boolean array to be formatted
   * @param sep the seperator between the single values of the double array,
   *            e.g. ','
   * @return a String representing the boolean array b
   */
  public static String format(boolean[] b, final String sep) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < b.length; i++) {
      if (i < b.length - 1) {
        buffer.append(format(b[i])).append(sep);
      }
      else {
        buffer.append(format(b[i]));
      }
    }
    return buffer.toString();
  }

  /**
   * Returns the prefix of the specidfied fileName (i.e. the name of the file
   * without extension).
   *
   * @param fileName the name of the file
   * @return the prefix of the specidfied fileName
   */
  public static String getFilePrefix(final String fileName) {
    final int index = fileName.lastIndexOf('.');
    if (index < 0) {
      return fileName;
    }
    return fileName.substring(0, index);
  }

  /**
   * Returns a new String array containing the same objects as are contained
   * in the given array.
   *
   * @param array an array to copy
   * @return the copied array
   */
  public static String[] copy(String[] array) {
    String[] copy = new String[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  /**
   * Returns a new double array containng the same objects as are contained in
   * the given array.
   *
   * @param array an array to copy
   * @return the copied array
   */
  public static double[] copy(double[] array) {
    double[] copy = new double[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return copy;
  }

  /**
   * Returns the unboxed double array of the given Object Double array.
   *
   * @param array the array to be unboxed
   * @return the unboxed double array
   */
  public static double[] unbox(Double[] array) {
    double[] unboxed = new double[array.length];
    // noinspection ManualArrayCopy
    for (int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i];
    }
    return unboxed;
  }

  /**
   * Returns the unboxed double array of the given Object Number array.
   *
   * @param array the array to be unboxed
   * @return the unboxed double array
   */
  public static double[] unbox(Number[] array) {
    double[] unboxed = new double[array.length];
    for (int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i].doubleValue();
    }
    return unboxed;
  }

  /**
   * Returns the unboxed float array of the given Object Number array.
   *
   * @param array the array to be unboxed
   * @return the unboxed float array
   */
  public static float[] unboxToFloat(Number[] array) {
    float[] unboxed = new float[array.length];
    for (int i = 0; i < unboxed.length; i++) {
      unboxed[i] = array[i].floatValue();
    }
    return unboxed;
  }

  /**
   * Returns the centroid as a RealVector object of the specified objects
   * stored in the given database. The objects belonging to the specified ids
   * must be instance of <code>RealVector</code>.
   *
   * @param database the database storing the objects
   * @param ids      the ids of the objects
   * @return the centroid of the specified objects stored in the given
   *         database
   * @throws IllegalArgumentException if the id list is empty
   */
  @SuppressWarnings("unchecked")
  public static RealVector centroid(Database<RealVector> database, Collection<Integer> ids) {
    if (ids.isEmpty()) {
      throw new IllegalArgumentException("Empty list of ids!");
    }

    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    for (int id : ids) {
      RealVector o = database.get(id);
      for (int j = 1; j <= dim; j++) {
        centroid[j - 1] += o.getValue(j).doubleValue();
      }
    }

    for (int i = 0; i < dim; i++) {
      centroid[i] /= ids.size();
    }

    RealVector o = database.get(ids.iterator().next());
    return o.newInstance(centroid);
  }

  /**
   * Returns the centroid w.r.t. the dimensions specified
   * by the given bitSet as a RealVector object of the specified objects
   * stored in the given database. The objects belonging to the specified ids
   * must be instance of <code>RealVector</code>.
   *
   * @param database the database storing the objects
   * @param ids      the ids of the objects
   * @param bitSet   the bitSet specifiying the dimensions to be considered
   * @return the centroid of the specified objects stored in the given
   *         database
   * @throws IllegalArgumentException if the id list is empty
   */
  @SuppressWarnings("unchecked")
  public static RealVector centroid(Database<RealVector> database, Collection<Integer> ids, BitSet bitSet) {
    if (ids.isEmpty()) {
      throw new IllegalArgumentException("Empty list of ids!");
    }

    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    for (int id : ids) {
      RealVector o = database.get(id);
      for (int j = 1; j <= dim; j++) {
        if (bitSet.get(j - 1)) {
          centroid[j - 1] += o.getValue(j).doubleValue();
        }
      }
    }

    for (int i = 0; i < dim; i++) {
      centroid[i] /= ids.size();
    }

    RealVector o = database.get(ids.iterator().next());
    return o.newInstance(centroid);
  }

  /**
   * Returns the centroid as a RealVector object of the specified database.
   * The objects must be instance of <code>RealVector</code>.
   *
   * @param database the database storing the objects
   * @return the centroid of the specified objects stored in the given
   *         database
   */
  @SuppressWarnings("unchecked")
  public static RealVector centroid(Database<RealVector> database) {
    if (database.size() == 0) {
      throw new IllegalArgumentException("Database is empty!");
    }
    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    Iterator<Integer> it = database.iterator();
    while (it.hasNext()) {
      RealVector o = database.get(it.next());
      for (int j = 1; j <= dim; j++) {
        centroid[j - 1] += o.getValue(j).doubleValue();
      }
    }

    for (int i = 0; i < dim; i++) {
      centroid[i] /= database.size();
    }
    RealVector o = database.get(database.iterator().next());
    return o.newInstance(centroid);
  }

  /**
   * Returns the centroid as a Vector object of the specified data matrix.
   *
   * @param data the data matrix, where the data vectors are column vectors
   * @return the centroid of the specified data matrix
   */
  public static Vector centroid(Matrix data) {
    int d = data.getRowDimensionality();
    int n = data.getColumnDimensionality();
    double[] centroid = new double[d];

    for (int i = 0; i < n; i++) {
      Vector x = data.getColumnVector(i);
      for (int j = 0; j < d; j++) {
        centroid[j] += x.get(j);
      }
    }

    for (int j = 0; j < d; j++) {
      centroid[j] /= n;
    }

    return new Vector(centroid);
  }

  /**
   * Determines the covariance matrix of the objects stored in the given
   * database.
   *
   * @param database the database storing the objects
   * @param ids      the ids of the objects
   * @return the covarianvce matrix of the specified objects
   */
  public static Matrix covarianceMatrix(Database<RealVector> database, List<Integer> ids) {
    // centroid
    RealVector centroid = centroid(database, ids);

    // covariance matrixArray
    int columns = centroid.getDimensionality();
    int rows = ids.size();

    double[][] matrixArray = new double[rows][columns];

    for (int i = 0; i < rows; i++) {
      RealVector obj = database.get(ids.get(i));
      for (int d = 0; d < columns; d++) {
        matrixArray[i][d] = obj.getValue(d + 1).doubleValue() - centroid.getValue(d + 1).doubleValue();
      }
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    return centeredMatrix.transpose().times(centeredMatrix);
  }

  /**
   * Determines the covariance matrix of the objects stored in the given
   * database.
   *
   * @param database the database storing the objects
   * @return the covarianvce matrix of the specified objects
   */
  public static Matrix covarianceMatrix(Database<RealVector> database) {
    // centroid
    RealVector centroid = centroid(database);

    // centered matrix
    int columns = centroid.getDimensionality();
    int rows = database.size();
    double[][] matrixArray = new double[rows][columns];

    Iterator<Integer> it = database.iterator();
    int i = 0;
    while (it.hasNext()) {
      RealVector obj = database.get(it.next());
      for (int d = 0; d < columns; d++) {
        matrixArray[i][d] = obj.getValue(d + 1).doubleValue() - centroid.getValue(d + 1).doubleValue();
      }
      i++;
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    // covariance matrix
    Matrix cov = centeredMatrix.transpose().times(centeredMatrix);
    cov = cov.times(1.0 / database.size());

    return cov;
  }

  /**
   * Determines the d x d covariance matrix of the given n x d data matrix.
   *
   * @param data the database storing the objects
   * @return the covarianvce matrix of the given data matrix.
   */
  public static Matrix covarianceMatrix(Matrix data) {
    // centroid
    Vector centroid = centroid(data);

    // centered matrix
    double[][] matrixArray = new double[data.getRowDimensionality()][data.getColumnDimensionality()];

    for (int i = 0; i < data.getRowDimensionality(); i++) {
      for (int j = 0; j < data.getColumnDimensionality(); j++) {
        matrixArray[i][j] = data.get(i, j) - centroid.get(i);
      }
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    // covariance matrix
    Matrix cov = centeredMatrix.times(centeredMatrix.transpose());
    cov = cov.times(1.0 / data.getColumnDimensionality());

    return cov;
  }


  /**
   * Determines the variances in each dimension of all
   * objects stored in the given database.
   *
   * @param database the database storing the objects
   * @return the variances in each dimension of all objects stored in the given database
   */
  public static double[] variances(Database<RealVector> database) {
    RealVector centroid = centroid(database);
    double[] variances = new double[centroid.getDimensionality()];

    for (int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
        RealVector o = database.get(it.next());
        double diff = o.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;
      }

      variances[d - 1] /= database.size();
    }
    return variances;
  }

  /**
   * Determines the variances in each dimension of the specified
   * objects stored in the given database.
   * Returns <code>variances(database, centroid(database, ids), ids)</code>
   *
   * @param database the database storing the objects
   * @param ids      the ids of the objects
   * @return the variances in each dimension of the specified objects
   */
  public static double[] variances(Database<RealVector> database, Collection<Integer> ids) {
    return variances(database, centroid(database, ids), ids);
  }

  /**
   * Determines the variances in each dimension of the specified
   * objects stored in the given database.
   *
   * @param database the database storing the objects
   * @param ids      the ids of the objects
   * @param centroid the centroid  or reference vector of the ids
   * @return the variances in each dimension of the specified objects
   */
  public static double[] variances(Database<RealVector> database, RealVector centroid, Collection<Integer> ids) {
    double[] variances = new double[centroid.getDimensionality()];

    for (int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      for (Integer id : ids) {
        RealVector o = database.get(id);
        double diff = o.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;
      }

      variances[d - 1] /= ids.size();
    }
    return variances;
  }

  /**
   * Determines the variances in each dimension of the specified
   * objects stored in the given database.
   *
   * @param database the database storing the objects
   * @param ids      the array of ids of the objects to be considered in each diemsnion
   * @param centroid the centroid  or reference vector of the ids
   * @return the variances in each dimension of the specified objects
   */
  public static double[] variances(Database<RealVector> database, RealVector centroid,
                                   Collection<Integer>[] ids) {
    double[] variances = new double[centroid.getDimensionality()];

    for (int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      Collection<Integer> ids_d = ids[d - 1];
      for (Integer neighborID : ids_d) {
        RealVector neighbor = database.get(neighborID);
        double diff = neighbor.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;
      }

      variances[d - 1] /= ids_d.size();
    }

    return variances;
  }

  /**
   * Determines the minimum and maximum values in each dimension of all
   * objects stored in the given database.
   *
   * @param database the database storing the objects
   * @return an array consisting of an array of the minimum and an array of the maximum
   *         values in each dimension
   *         of all objects stored in the given database
   */
  public static double[][] min_max(Database<RealVector> database) {
    int dim = database.dimensionality();
    double[] min = new double[dim];
    double[] max = new double[dim];
    Arrays.fill(min, Double.MAX_VALUE);
    Arrays.fill(max, -Double.MAX_VALUE);

    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      RealVector o = database.get(it.next());
      for (int d = 1; d <= dim; d++) {
        double v = o.getValue(d).doubleValue();
        min[d] = Math.min(min[d], v);
        max[d] = Math.min(max[d], v);
      }
    }
    return new double[][]{min, max};
  }

  /**
   * Returns a new <code>Double</code> array initialized to the values
   * represented by the specified <code>String</code> and separated by
   * comma, as performed by the <code>valueOf</code> method of class
   * <code>Double</code>.
   *
   * @param s the string to be parsed.
   * @return a new <code>Double</code> array represented by s
   */
  public static double[] parseDoubles(String s) {
    List<Double> result = new ArrayList<Double>();
    StringTokenizer tokenizer = new StringTokenizer(s, ",");
    while (tokenizer.hasMoreTokens()) {
      String d = tokenizer.nextToken();
      result.add(Double.parseDouble(d));
    }
    return unbox(result.toArray(new Double[result.size()]));
  }

  /**
   * Returns a new <code>Float</code> array initialized to the values
   * represented by the specified <code>String</code> and separated by
   * comma, as performed by the <code>valueOf</code> method of class
   * <code>Float</code>.
   *
   * @param s the string to be parsed.
   * @return a new <code>Float</code> array represented by s
   */
  public static float[] parseFloats(String s) {
    List<Float> result = new ArrayList<Float>();
    StringTokenizer tokenizer = new StringTokenizer(s, ",");
    while (tokenizer.hasMoreTokens()) {
      String d = tokenizer.nextToken();
      result.add(Float.parseFloat(d));
    }
    return unboxToFloat(result.toArray(new Float[result.size()]));
  }

  /**
   * Converts the specified list of double objects to a list of float objects.
   *
   * @param values the list of double objects to be converted
   * @return the converted list of float objects
   */
  public static List<Float> convertToFloat(List<Double> values) {
    List<Float> result = new ArrayList<Float>(values.size());
    for (Double value : values) {
      result.add(new Float(value));
    }
    return result;
  }

  /**
   * Converts the specified array of doubles to an array of floats.
   *
   * @param values the array of doubles to be converted
   * @return the converted array of floats
   */
  public static float[] convertToFloat(double[] values) {
    float[] result = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (float) values[i];
    }
    return result;
  }

  /**
   * Converts the specified array of doubles to an array of floats.
   *
   * @param values the array of doubles to be converted
   * @return the converted array of floats
   */
  public static double[] convertToDoubles(float[] values) {
    double[] result = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i];
    }
    return result;
  }

  /**
   * Converts the specified list of Double objects to an array of doubles.
   *
   * @param values the list of Double objects to be converted
   * @return the converted array of doubles
   */
  public static double[] convertToDoubles(List<Double> values) {
    double[] result = new double[values.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = values.get(i);
    }
    return result;
  }

  /**
   * Provides a script-text for a gnuplot script to use for transposed view of
   * a specific file of given size of data set.
   *
   * @param filename       the filename of the transposed file to be plotted
   * @param datasetSize    the size of the transposed data set
   * @param dimensionality the dimensionality of the transposed data set
   * @return a script-text for a gnuplot script to use for transposed view of
   *         a specific file of given size of data set
   */
  public static String transposedGnuplotScript(String filename, int datasetSize, int dimensionality) {
    StringBuffer script = new StringBuffer();
    // script.append("set terminal pbm color;\n");
    script.append("set nokey\n");
    script.append("set data style linespoints\n");
    script.append("set xlabel \"attribute\"\n");
    script.append("show xlabel\n");
    script.append("plot [0:");
    script.append(datasetSize - 1).append("] []");
    for (int p = 1; p <= dimensionality; p++) {
      script.append("\"").append(filename).append("\" using ").append(p);
      if (p < dimensionality) {
        script.append(", ");
      }
    }
    script.append("\npause -1");
    return script.toString();
  }

  /**
   * Returns a new instance of the given type for the specified className.
   * <p/> If the Class for className is not found, the instantiation is tried
   * using the package of the given type as package of the given className.
   *
   * @param type      desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws UnableToComplyException if the instantiation cannot be performed successfully
   */
  public static <T> T instantiate(Class<T> type, String className) throws UnableToComplyException {
    T instance;
    try {
      try {
        instance = type.cast(Class.forName(className).newInstance());
      }
      catch (ClassNotFoundException e) {
        // try package of type
        instance = type.cast(Class.forName(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch (InstantiationException e) {
      throw new UnableToComplyException("InstantiationException occurred!", e);
    }
    catch (IllegalAccessException e) {
      throw new UnableToComplyException("IllegalAccessException occurred!", e);
    }
    catch (ClassNotFoundException e) {
      throw new UnableToComplyException("ClassNotFoundException occurred!", e);
    }
    catch (ClassCastException e) {
      throw new UnableToComplyException("ClassCastException occurred!", e);
    }
    return instance;
  }

  /**
   * Provides a status report line with leading carriage return. Suitable for
   * density based algorithms, since the number of found clusters is counted.
   *
   * @param progress the progress status
   * @param clusters current number of clusters
   * @return a status report line with leading carriage return
   */
  public static String status(Progress progress, int clusters) {
    StringBuffer status = new StringBuffer();
    status.append("\r");
    status.append(progress.toString());
    status.append(" Number of clusters: ");
    status.append(clusters);
    status.append(".                           ");
    return status.toString();
  }

  /**
   * Provides a status report line with leading carriage return.
   *
   * @param progress the progress status
   * @return a status report line with leading carriage return
   */
  public static String status(Progress progress) {
    StringBuffer status = new StringBuffer();
    status.append("\r");
    status.append(progress.toString());
    status.append("                           ");
    return status.toString();
  }

  /**
   * Prints the given list to the specified PrintStream. The list entries are
   * separated by the specified separator. The last entry is not followed by a
   * separator. Thus, if a newline is used as separator, it might make sense
   * to print a newline to the PrintStream after calling this method.
   *
   * @param list      the list to be printed
   * @param separator the separator to separate entries of the list
   * @param out       the target PrintStream
   */
  public static <O extends Object> void print(List<O> list, String separator, PrintStream out) {
    for (Iterator<O> iter = list.iterator(); iter.hasNext();) {
      out.print(iter.next());
      if (iter.hasNext()) {
        out.print(separator);
      }
    }
  }

  /**
   * Returns the index of the maximum of the given values. If no value is
   * bigger than the first, the index of the first entry is returned.
   *
   * @param values the values to find the index of the maximum
   * @return the index of the maximum in the given values
   * @throws ArrayIndexOutOfBoundsException if <code>values.length==0</code>
   */
  public static int getIndexOfMaximum(double[] values) throws ArrayIndexOutOfBoundsException {
    int index = 0;
    double max = values[index];
    for (int i = 0; i < values.length; i++) {
      if (values[i] > max) {
        max = values[i];
        index = i;
      }
    }
    return index;
  }

  /**
   * Retrieves all class labels within the database.
   *
   * @param database the database to be scanned for class labels
   * @return a set comprising all class labels that are currently set in the
   *         database
   */
  @SuppressWarnings("unchecked")
  public static SortedSet<ClassLabel> getClassLabels(Database database) {
    if (!database.isSetForAllObjects(AssociationID.CLASS)) {
      throw new IllegalStateException("AssociationID " + AssociationID.CLASS.getName() + " is not set.");
    }
    SortedSet<ClassLabel> labels = new TreeSet<ClassLabel>();
    for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      labels.add((ClassLabel) database.getAssociation(AssociationID.CLASS, iter.next()));
    }
    return labels;
  }

  /**
   * Returns an array that contains all elements of the first parameter array that
   * are not contained by the second parameter array. The first parameter array must at
   * least be as long as the second. The second must not contain entries that
   * are not contained by the first.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first parameter array
   * @return an array that contains all elements of the first parameter array that
   *         are not contained by the second parameter array
   * @throws IllegalArgumentException if the first array, <code>complete</code> is not as long as
   *                                  the second array, <code>part</code> or the second,
   *                                  <code>part</code>, contains entries that are not contained
   *                                  by the first, <code>complete</code>
   */
  public static String[] parameterDifference(String[] complete, String[] part) throws IllegalArgumentException {
    if (complete.length < part.length) {
      throw new IllegalArgumentException("first array must be at least as long as second array.");
    }

    if (complete.length == 0) {
      return new String[0];
    }

    List<String> completeArray = new ArrayList<String>();
    for (int i = 0; i < complete.length; i++) {
      String param = complete[i];
      if (param.startsWith(OptionHandler.OPTION_PREFIX)) {
        if (i < complete.length - 1) {
          String key = complete[i + 1];
          if (! key.startsWith(OptionHandler.OPTION_PREFIX)) {
            completeArray.add(param + " " + key);
            i++;
          }
          else {
            completeArray.add(param);
          }
        }
      }
    }

    List<String> partArray = new ArrayList<String>();
    for (int i = 0; i < part.length; i++) {
      String param = part[i];
      if (param.startsWith(OptionHandler.OPTION_PREFIX)) {
        if (i < part.length - 1) {
          String key = part[i + 1];
          if (! key.startsWith(OptionHandler.OPTION_PREFIX)) {
            partArray.add(param + " " + key);
            i++;
          }
          else {
            partArray.add(param);
          }
        }
      }
    }

    Pattern pattern = Pattern.compile(" ");
    List<String> result = new ArrayList<String>();
    int first = 0;
    int second = 0;
    while (first < completeArray.size() && second < partArray.size()) {
      if (completeArray.get(first).equals(partArray.get(second))) {
        first++;
        second++;
      }
      else {
        String[] params = pattern.split(completeArray.get(first));
        for (String p : params) {
          result.add(p);
        }
        first++;
      }
    }
    if (second < partArray.size()) {
      throw new IllegalArgumentException("second array contains entries that are not contained by the first array.");
    }
    while (first < completeArray.size()) {
      String[] params = pattern.split(completeArray.get(first));
      for (String p : params) {
        result.add(p);
      }
      first++;
    }


    String[] resultArray = new String[result.size()];
    return result.toArray(resultArray);
  }

  /**
   * Provides a string describing the restriction to implement or extend
   * the specified class.
   * <p/>
   * The message has a structure like follows:
   * <pre>
   * (implementing typeName -- available classes:
   * -->class1.name
   * -->class2.name
   * )
   * </pre>
   *
   * @param type the type restricting the possible classes
   * @return a description listing all available classes
   *         restricted by the specified class
   */
  @SuppressWarnings("unchecked")
  public static String restrictionString(Class type) {
    StringBuilder msg = new StringBuilder();
    msg.append('(');
    if (type.isInterface()) {
      msg.append("implementing ");
    }
    else {
      msg.append("extending ");
    }
    msg.append(type.getName());
    Class[] classes = implementingClasses(type);
    if (logger.debug()) {
      logger.debugFinest("Classes for " + type.getName() + ": " + Arrays.asList(classes).toString());
    }
    if (classes.length > 0) {
      msg.append(" -- available classes:\n");
      for (Class c : classes) {
        msg.append("-->");
        msg.append(c.getName());
        msg.append('\n');
      }
    }
    msg.append(')');
    return msg.toString();
  }

  /**
   * Provides all classes currently known by the Launcher
   * that are instance of the specified type
   * and that are instantiable by the default constructor.
   *
   * @param type the common superclass or interface
   *             of the required classes
   * @return all classes currently known by the Launcher
   *         that are instance of the specified type
   *         and that are instantiable by the default constructor
   */
  @SuppressWarnings("unchecked")
  public static Class[] implementingClasses(Class type) {
    List<Class> classes = new ArrayList<Class>();
    Package[] packages = Package.getPackages();
    if (logger.debug()) {
      logger.debugFinest("found packages: " + Arrays.asList(packages).toString());
    }
    for (Package p : packages) {
      if (logger.debug()) {
        logger.debugFinest(p.getName());
      }
      Class[] classesInPackage = classesInPackage(p);
      int added = 0;
      for (Class c : classesInPackage) {
        if (type.isAssignableFrom(c)) {
          if (logger.debug()) {
            logger.debugFinest(type.getName() + " is assignable from " + c.getName());
          }
          classes.add(c);
          added++;
        }
      }
      if (logger.debug()) {
        if (added != classesInPackage.length) {
          for (Class c : classesInPackage) {
            if (!classes.contains(c)) {
              logger.debugFinest(type.getName() + " assignable from " + c.getName() + ": " + type.isAssignableFrom(c));
            }
          }
        }
      }
    }
    Class[] result = new Class[classes.size()];
    return classes.toArray(result);
  }

  /**
   * Provides all classes in the specified package
   * as currently present in the Launcher.
   * Only those classes are included
   * that can be instantiated per default constructor.
   *
   * @param p the package to retrieve classes for
   * @return all classes in the specified package
   */
  @SuppressWarnings("unchecked")
  public static Class[] classesInPackage(Package p) {
    List<Class> classes = new ArrayList<Class>();
    final String CLASS_SUFFIX = ".class";
    String pname = p.getName();
    URL url = Launcher.class.getResource(pname);
    if (url == null) {
      pname = pname.replace('.', '/');
      if (!pname.startsWith("/")) {
        pname = "/" + pname;
      }
      url = Launcher.class.getResource(pname);
    }
    if (url != null) {
      File dir = new File(url.getFile());
      if (dir.exists()) {
        String[] files = dir.list();
        for (String f : files) {
          if (f.endsWith(CLASS_SUFFIX)) {
            // remove the .class extension
            String classname = f.substring(0, f.length() - 6);
            try {
              if (logger.debug()) {
                logger.debugFinest("classname: " + classname);
              }
              Class c = Class.forName(p.getName() + "." + classname);
              if (logger.debug()) {
                logger.debugFinest("class: " + c.getName());
              }
              Object o = c.newInstance();
              if (logger.debug()) {
                logger.debugFinest("object class: " + o.getClass().getName());
              }
              classes.add(c);
            }
            catch (Exception e) {
              if (logger.debug()) {
                logger.debugFinest(e.getMessage() + "\n");
              }
            }
          }
        }
      }
    }
    else {
      if (logger.debug()) {
        logger.debugFinest("No resource available for name: \"" + pname + "\"\n");
      }
    }
    Class[] result = new Class[classes.size()];
    return classes.toArray(result);
  }

  /**
   * Returns a string representation of the specified bit set.
   *
   * @param bitSet the bitSet
   * @param dim    the overall dimensionality of the bit set
   * @param sep   the separator
   * @return a string representation of the specified bit set.
   */
  public static String format(BitSet bitSet, int dim, String sep) {
    StringBuffer msg = new StringBuffer();

    for (int d = 0; d < dim; d++) {
      if (d > 0) {
        msg.append(sep);
      }
      if (bitSet.get(d)) {
        msg.append("1");
      }
      else {
        msg.append("0");
      }
    }

    return msg.toString();
  }

  /**
   * Returns a string representation of the specified bit set.
   *
   * @param dim    the overall dimensionality of the bit set
   * @param bitSet the bitSet
   * @return a string representation of the specified bit set.
   */
  public static String format(int dim, BitSet bitSet) {
    return format(bitSet, dim, ", ");
  }

  /**
   * Provides the intersection of the two specified sets in the given result set.
   *
   * @param s1     the first set
   * @param s2     the second set
   * @param result the result set
   */
  public static <O extends Object> void intersection(Set<O> s1, Set<O> s2, Set<O> result) {
    for (O object : s1) {
      if (s2.contains(object)) {
        result.add(object);
      }
    }
  }

  /**
   * Converts the specified integer value into a bit representation,
   * where bit 0 denotes 2^0, bit 1 denotes 2^1 etc..
   *
   * @param n              the integer value to be converted
   * @return the specified integer value into a bit representation
   */
  public static BitSet int2Bit(int n) {
    BitSet result = new BitSet();
    int i = 0;
    while (n > 0) {
      boolean rest = (n % 2 == 1);
      if (rest) {
        result.set(i);
      }
      n = n / 2;
      i++;
    }
    return result;
  }

}