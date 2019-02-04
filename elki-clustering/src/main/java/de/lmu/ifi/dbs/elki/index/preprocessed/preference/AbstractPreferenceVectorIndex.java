/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;

/**
 * Abstract base class for preference vector based algorithms.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <NV> Number vector
 */
public abstract class AbstractPreferenceVectorIndex<NV extends NumberVector> extends AbstractPreprocessorIndex<NV, long[]> implements PreferenceVectorIndex<NV> {
  /**
   * Constructor.
   * 
   * @param relation Relation to use
   */
  public AbstractPreferenceVectorIndex(Relation<NV> relation) {
    super(relation);
  }

  @Override
  public long[] getPreferenceVector(DBIDRef objid) {
    if(storage == null) {
      initialize();
    }
    return storage.get(objid);
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @navassoc - create - AbstractPreferenceVectorIndex
   */
  public abstract static class Factory<V extends NumberVector> implements PreferenceVectorIndex.Factory<V> {
    @Override
    public abstract PreferenceVectorIndex<V> instantiate(Relation<V> relation);

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }
  }
}
