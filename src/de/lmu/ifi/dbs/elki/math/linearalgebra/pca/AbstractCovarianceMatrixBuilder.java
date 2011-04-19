package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Abstract class with the task of computing a Covariance matrix to be used in PCA.
 * Mostly the specification of an interface.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector class in use
 * @param <D> Distance type
 */
public abstract class AbstractCovarianceMatrixBuilder<V extends NumberVector<?, ?>, D extends NumberDistance<D,?>> implements Parameterizable, CovarianceMatrixBuilder<V, D> {
  @Override
  public Matrix processDatabase(Relation<? extends V> database) {
    return processIds(database.getDBIDs(), database);
  }

  @Override
  public abstract Matrix processIds(DBIDs ids, Relation<? extends V> database);

  @Override
  public Matrix processQueryResults(Collection<DistanceResultPair<D>> results, Relation<? extends V> database, int k) {
    ModifiableDBIDs ids = DBIDUtil.newArray(k);
    int have = 0;
    for(Iterator<DistanceResultPair<D>> it = results.iterator(); it.hasNext() && have < k; have++) {
      ids.add(it.next().getID());
    }
    return processIds(ids, database);
  }

  @Override
  final public Matrix processQueryResults(Collection<DistanceResultPair<D>> results, Relation<? extends V> database) {
    return processQueryResults(results, database, results.size());
  }
  
  // TODO: Allow KNNlist to avoid building the DBID array?
}