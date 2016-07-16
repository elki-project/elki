package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Abstract class with the task of computing a Covariance matrix to be used in PCA.
 * Mostly the specification of an interface.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public abstract class AbstractCovarianceMatrixBuilder implements CovarianceMatrixBuilder {
  @Override
  public Matrix processDatabase(Relation<? extends NumberVector> database) {
    return processIds(database.getDBIDs(), database);
  }

  @Override
  public abstract Matrix processIds(DBIDs ids, Relation<? extends NumberVector> database);

  @Override
  public Matrix processQueryResults(DoubleDBIDList results, Relation<? extends NumberVector> database, int k) {
    ModifiableDBIDs ids = DBIDUtil.newArray(k);
    int have = 0;
    for(DBIDIter it = results.iter(); it.valid() && have < k; it.advance(), have++) {
      ids.add(it);
    }
    return processIds(ids, database);
  }

  @Override
  public final Matrix processQueryResults(DoubleDBIDList results, Relation<? extends NumberVector> database) {
    return processQueryResults(results, database, results.size());
  }
}