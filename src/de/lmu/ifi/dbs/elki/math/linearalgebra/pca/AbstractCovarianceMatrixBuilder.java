package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

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
 */
public abstract class AbstractCovarianceMatrixBuilder<V extends NumberVector<? extends V, ?>> implements Parameterizable, CovarianceMatrixBuilder<V> {
  @Override
  public Matrix processDatabase(Relation<? extends V> database) {
    return processIds(database.getDBIDs(), database);
  }

  @Override
  public abstract Matrix processIds(DBIDs ids, Relation<? extends V> database);

  @Override
  public <D extends NumberDistance<?, ?>> Matrix processQueryResults(Collection<? extends DistanceResultPair<D>> results, Relation<? extends V> database, int k) {
    ModifiableDBIDs ids = DBIDUtil.newArray(k);
    int have = 0;
    for(Iterator<? extends DistanceResultPair<D>> it = results.iterator(); it.hasNext() && have < k; have++) {
      ids.add(it.next().getDBID());
    }
    return processIds(ids, database);
  }

  @Override
  final public <D extends NumberDistance<?, ?>> Matrix processQueryResults(Collection<? extends DistanceResultPair<D>> results, Relation<? extends V> database) {
    return processQueryResults(results, database, results.size());
  }
  
  // TODO: Allow KNNlist to avoid building the DBID array?
}