package de.lmu.ifi.dbs.elki.utilities;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class with Database-related utility functions such as centroid computation,
 * covariances etc.
 * 
 * @author Erich Schubert
 */
public final class DatabaseUtil {
  /**
   * Returns the centroid as a NumberVector object of the specified objects stored
   * in the given database. The objects belonging to the specified ids must be
   * instance of <code>NumberVector</code>.
   * 
   * @param <V> Vector type
   * @param database the database storing the objects
   * @param ids the ids of the objects
   * @return the centroid of the specified objects stored in the given database
   * @throws IllegalArgumentException if the id list is empty
   */
  public static <V extends NumberVector<V, ?>> V centroid(Database<V> database, Collection<Integer> ids) {
    if(ids.isEmpty()) {
      throw new IllegalArgumentException("Cannot compute a centroid, because of empty list of ids!");
    }

    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    for(int id : ids) {
      V o = database.get(id);
      for(int j = 1; j <= dim; j++) {
        centroid[j - 1] += o.getValue(j).doubleValue();
      }
    }
    double size = ids.size();
    for(int i = 0; i < dim; i++) {
      centroid[i] /= size;
    }

    V o = database.get(ids.iterator().next());
    return o.newInstance(centroid);
  }

  /**
   * Returns the centroid w.r.t. the dimensions specified by the given BitSet as
   * a NumberVector object of the specified objects stored in the given database.
   * The objects belonging to the specified ids must be instance of
   * <code>NumberVector</code>.
   * 
   * @param <V> Vector type
   * @param database the database storing the objects
   * @param ids the identifiable objects
   * @param bitSet the bitSet specifying the dimensions to be considered
   * @return the centroid of the specified objects stored in the given database
   * @throws IllegalArgumentException if the id list is empty
   */
  public static <V extends NumberVector<V, ?>> V centroid(Database<V> database, Collection<Integer> ids, BitSet bitSet) {
    if(ids.isEmpty()) {
      throw new IllegalArgumentException("Cannot compute a centroid, because of empty list of ids!");
    }

    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    for(Integer id : ids) {
      V o = database.get(id);
      for(int j = 1; j <= dim; j++) {
        if(bitSet.get(j - 1)) {
          centroid[j - 1] += o.getValue(j).doubleValue();
        }
      }
    }

    double size = ids.size();
    for(int i = 0; i < dim; i++) {
      centroid[i] /= size;
    }

    V o = database.get(ids.iterator().next());
    return o.newInstance(centroid);
  }

  /**
   * Returns the centroid w.r.t. the dimensions specified by the given BitSet as
   * a NumberVector object of the specified objects stored in the given database.
   * The objects belonging to the specified ids must be instance of
   * <code>NumberVector</code>.
   * 
   * @param <V> Vector type
   * @param database the database storing the objects
   * @param iter iterator over the identifiable objects
   * @param bitSet the bitSet specifying the dimensions to be considered
   * @return the centroid of the specified objects stored in the given database
   * @throws IllegalArgumentException if the id list is empty
   */
  public static <V extends NumberVector<V, ?>> V centroid(Database<V> database, Iterator<Integer> iter, BitSet bitSet) {
    if(!iter.hasNext()) {
      throw new IllegalArgumentException("Cannot compute a centroid, because of empty list of ids!");
    }

    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    int size = 0;
    // we need to "cache" one o for the newInstance method, since we can't clone
    // the iterator.
    V o = null;
    while(iter.hasNext()) {
      Integer id = iter.next();
      size++;
      o = database.get(id);
      for(int j = 1; j <= dim; j++) {
        if(bitSet.get(j - 1)) {
          centroid[j - 1] += o.getValue(j).doubleValue();
        }
      }
    }

    for(int i = 0; i < dim; i++) {
      centroid[i] /= size;
    }

    return o.newInstance(centroid);
  }

  /**
   * Returns the centroid as a NumberVector object of the specified database. The
   * objects must be instance of <code>NumberVector</code>.
   * 
   * @param <O> Vector type
   * @param database the database storing the objects
   * @return the centroid of the specified objects stored in the given database
   * @throws IllegalArgumentException if the database is empty
   */
  public static <O extends NumberVector<O, ?>> O centroid(Database<O> database) {
    if(database == null || database.size() == 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }
    int dim = database.dimensionality();
    double[] centroid = new double[dim];

    Iterator<Integer> it = database.iterator();
    while(it.hasNext()) {
      NumberVector<O, ?> o = database.get(it.next());
      for(int j = 1; j <= dim; j++) {
        centroid[j - 1] += o.getValue(j).doubleValue();
      }
    }

    double size = database.size();
    for(int i = 0; i < dim; i++) {
      centroid[i] /= size;
    }
    O o = database.get(database.iterator().next());
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
    double n = data.getColumnDimensionality();
    double[] centroid = new double[d];

    for(int i = 0; i < n; i++) {
      Vector x = data.getColumnVector(i);
      for(int j = 0; j < d; j++) {
        centroid[j] += x.get(j);
      }
    }

    for(int j = 0; j < d; j++) {
      centroid[j] /= n;
    }

    return new Vector(centroid);
  }

  /**
   * Determines the covariance matrix of the objects stored in the given
   * database.
   * 
   * @param <V> Vector type
   * @param database the database storing the objects
   * @param ids the ids of the objects
   * @return the covariance matrix of the specified objects
   */
  public static <V extends NumberVector<V, ?>> Matrix covarianceMatrix(Database<V> database, Collection<Integer> ids) {
    // centroid
    V centroid = centroid(database, ids);

    // covariance matrixArray
    int columns = centroid.getDimensionality();
    int rows = ids.size();

    double[][] matrixArray = new double[rows][columns];

    int i = 0;
    for(Iterator<Integer> it = ids.iterator(); it.hasNext(); i++) {
      NumberVector<?, ?> obj = database.get(it.next());
      for(int d = 0; d < columns; d++) {
        matrixArray[i][d] = obj.getValue(d + 1).doubleValue() - centroid.getValue(d + 1).doubleValue();
      }
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    return centeredMatrix.transposeTimes(centeredMatrix);
  }

  /**
   * Determines the covariance matrix of the objects stored in the given
   * database.
   * 
   * @param <O> Vector type
   * @param database the database storing the objects
   * @return the covariance matrix of the specified objects
   */
  public static <O extends NumberVector<O, ?>> Matrix covarianceMatrix(Database<O> database) {
    // centroid
    O centroid = centroid(database);

    return covarianceMatrix(database, centroid);
  }

  /**
   * <p>
   * Determines the covariance matrix of the objects stored in the given
   * database w.r.t. the given centroid.
   * </p>
   * 
   * @param <O> Vector type
   * @param database the database storing the objects
   * @param centroid the centroid of the database
   * @return the covariance matrix of the specified objects
   */
  public static <O extends NumberVector<O,?>> Matrix covarianceMatrix(Database<O> database, O centroid) {

    // centered matrix
    int columns = centroid.getDimensionality();
    int rows = database.size();
    double[][] matrixArray = new double[rows][columns];

    Iterator<Integer> it = database.iterator();
    int i = 0;
    while(it.hasNext()) {
      NumberVector<?,?> obj = database.get(it.next());
      for(int d = 0; d < columns; d++) {
        matrixArray[i][d] = obj.getValue(d + 1).doubleValue() - centroid.getValue(d + 1).doubleValue();
      }
      i++;
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    // covariance matrix
    Matrix cov = centeredMatrix.transposeTimes(centeredMatrix);
    cov = cov.times(1.0 / database.size());

    return cov;
  }

  /**
   * Determines the d x d covariance matrix of the given n x d data matrix.
   * 
   * @param data the database storing the objects
   * @return the covariance matrix of the given data matrix.
   */
  public static Matrix covarianceMatrix(Matrix data) {
    // centroid
    Vector centroid = centroid(data);

    // centered matrix
    double[][] matrixArray = new double[data.getRowDimensionality()][data.getColumnDimensionality()];

    for(int i = 0; i < data.getRowDimensionality(); i++) {
      for(int j = 0; j < data.getColumnDimensionality(); j++) {
        matrixArray[i][j] = data.get(i, j) - centroid.get(i);
      }
    }
    Matrix centeredMatrix = new Matrix(matrixArray);
    // covariance matrix
    Matrix cov = centeredMatrix.timesTranspose(centeredMatrix);
    cov = cov.times(1.0 / data.getColumnDimensionality());

    return cov;
  }

  /**
   * Determines the variances in each dimension of all objects stored in the
   * given database.
   * 
   * @param <O> Vector type
   * @param database the database storing the objects
   * @return the variances in each dimension of all objects stored in the given
   *         database
   */
  public static <O extends NumberVector<O, ?>> double[] variances(Database<O> database) {
    O centroid = centroid(database);
    double[] variances = new double[centroid.getDimensionality()];

    for(int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      for(Iterator<Integer> it = database.iterator(); it.hasNext();) {
        NumberVector<?, ?> o = database.get(it.next());
        double diff = o.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;
      }

      variances[d - 1] /= database.size();
    }
    return variances;
  }

  /**
   * Determines the variances in each dimension of the specified objects stored
   * in the given database. Returns
   * <code>variances(database, centroid(database, ids), ids)</code>
   * 
   * @param <V> Vector type
   * @param database the database storing the objects
   * @param ids the ids of the objects
   * @return the variances in each dimension of the specified objects
   */
  public static <V extends NumberVector<V, ?>> double[] variances(Database<V> database, Collection<Integer> ids) {
    return variances(database, centroid(database, ids), ids);
  }

  /**
   * Determines the variances in each dimension of the specified objects stored
   * in the given database.
   * 
   * @param <V> Vector type
   * @param database the database storing the objects
   * @param ids the ids of the objects
   * @param centroid the centroid or reference vector of the ids
   * @return the variances in each dimension of the specified objects
   */
  public static <V extends NumberVector<V,?>> double[] variances(Database<V> database, V centroid, Collection<Integer> ids) {
    double[] variances = new double[centroid.getDimensionality()];

    for(int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      for(Integer id : ids) {
        V o = database.get(id);
        double diff = o.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;
      }

      variances[d - 1] /= ids.size();
    }
    return variances;
  }

  /**
   * Determines the variances in each dimension of the specified objects stored
   * in the given database.
   * 
   * @param database the database storing the objects
   * @param ids the array of ids of the objects to be considered in each
   *        dimension
   * @param centroid the centroid or reference vector of the ids
   * @return the variances in each dimension of the specified objects
   */
  public static double[] variances(Database<NumberVector<?, ?>> database, NumberVector<?, ?> centroid, Collection<Integer>[] ids) {
    double[] variances = new double[centroid.getDimensionality()];

    for(int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      Collection<Integer> ids_d = ids[d - 1];
      for(Integer neighborID : ids_d) {
        NumberVector<?, ?> neighbor = database.get(neighborID);
        double diff = neighbor.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;
      }

      variances[d - 1] /= ids_d.size();
    }

    return variances;
  }

  /**
   * Determines the minimum and maximum values in each dimension of all objects
   * stored in the given database.
   * 
   * @param database the database storing the objects
   * @return Minimum and Maximum vector for the hyperrectangle
   */
  public static <NV extends NumberVector<NV,?>> Pair<NV, NV> computeMinMax(Database<NV> database) {
    int dim = database.dimensionality();
    double[] mins = new double[dim];
    double[] maxs = new double[dim];
    for (int i = 0; i < dim; i++) {
      mins[i] = -Double.MAX_VALUE;
      maxs[i] = Double.MAX_VALUE;
    }
    for(Integer it : database) {
      NV o = database.get(it);
      for(int d = 1; d <= dim; d++) {
        double v = o.getValue(d).doubleValue();
        mins[d] = Math.min(mins[d],v);
        maxs[d] = Math.max(maxs[d],v);
      }
    }
    NV prototype = database.get(database.iterator().next());
    NV min = prototype.newInstance(mins);
    NV max = prototype.newInstance(maxs);
    return new Pair<NV,NV>(min, max);
  }

  /**
   * Retrieves all class labels within the database.
   * 
   * @param database the database to be scanned for class labels
   * @return a set comprising all class labels that are currently set in the
   *         database
   */
  public static SortedSet<ClassLabel> getClassLabels(Database<?> database) {
    if(!database.isSetForAllObjects(AssociationID.CLASS)) {
      throw new IllegalStateException("AssociationID " + AssociationID.CLASS.getName() + " is not set.");
    }
    SortedSet<ClassLabel> labels = new TreeSet<ClassLabel>();
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      // noinspection unchecked
      labels.add(database.getAssociation(AssociationID.CLASS, iter.next()));
    }
    return labels;
  }

  /**
   * Do a cheap guess at the databases object class.
   * 
   * @param <O> Restriction type
   * @param database Database
   * @return Class of first object in the Database.
   */
  @SuppressWarnings("unchecked")
  public static <O extends DatabaseObject> Class<? extends O> guessObjectClass(Database<O> database) {
    for(Integer id : database) {
      return (Class<? extends O>) database.get(id).getClass();
    }
    return null;
  }

  /**
   * Do a full inspection of the database to find the base object class.
   * 
   * Note: this can be an abstract class or interface!
   * 
   * TODO: Implement a full search for shared superclasses.
   * But since currently the databases will always use only once class, this is not yet implemented.
   * 
   * @param <O> Restriction type
   * @param database Database
   * @return Superclass of all objects in the database
   */
  @SuppressWarnings("unchecked")
  public static <O extends DatabaseObject> Class<? extends DatabaseObject> getBaseObjectClassExpensive(Database<O> database) {
    final Class<DatabaseObject> databaseObjectClass = DatabaseObject.class;
    List<Class<? extends DatabaseObject>> candidates = new ArrayList<Class<? extends DatabaseObject>>();
    Iterator<Integer> iditer = database.iterator();
    // empty database?!
    if (!iditer.hasNext()) {
      return null;
    }
    // put first class into result set.
    candidates.add(database.get(iditer.next()).getClass());
    // other objects
    while (iditer.hasNext()) {
      Class<? extends DatabaseObject> newcls = database.get(iditer.next()).getClass();
      // validate all candidates
      Iterator<Class<? extends DatabaseObject>> ci = candidates.iterator();
      while (ci.hasNext()) {
        Class<? extends DatabaseObject> cand = ci.next();
        if (cand.isAssignableFrom(newcls)) {
          continue;
        }
        // TODO: resolve conflicts by finding all superclasses!
        // Does this code here work?
        for (Class<?> interf : cand.getInterfaces()) {
          if (interf.isAssignableFrom(databaseObjectClass)) {
            candidates.add((Class<? extends DatabaseObject>) interf);
          }
        }
        if (cand.getSuperclass().isAssignableFrom(databaseObjectClass)) {
          candidates.add((Class<? extends DatabaseObject>) cand.getSuperclass());
        }
        ci.remove();
      }
    }
    // if we have any candidates left ...
    if(candidates != null && candidates.size() > 0) {
      // remove subclasses
      Iterator<Class<? extends DatabaseObject>> ci = candidates.iterator();
      while (ci.hasNext()) {
        Class<? extends DatabaseObject> cand = ci.next();
        for (Class<? extends DatabaseObject> oc : candidates) {
          if (oc != cand && cand.isAssignableFrom(oc)) {
            ci.remove();
            break;
          }
        }
      }
      assert(candidates.size() > 0);
      try {
        return candidates.get(0);
      }
      catch(ClassCastException e) {
        // ignore, and retry with next
      }
    }
    // no resulting class.
    return null;
  }
}
