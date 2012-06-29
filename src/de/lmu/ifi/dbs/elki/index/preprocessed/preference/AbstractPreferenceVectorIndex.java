package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Abstract base class for preference vector based algorithms.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Number vector
 */
public abstract class AbstractPreferenceVectorIndex<NV extends NumberVector<?, ?>> extends AbstractPreprocessorIndex<NV, BitSet> implements PreferenceVectorIndex<NV> {
  /**
   * Constructor.
   * 
   * @param relation Relation to use
   */
  public AbstractPreferenceVectorIndex(Relation<NV> relation) {
    super(relation);
  }

  /**
   * Preprocessing step.
   */
  abstract protected void preprocess();

  @Override
  public BitSet getPreferenceVector(DBIDRef objid) {
    if(storage == null) {
      preprocess();
    }
    return storage.get(objid);
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractPreferenceVectorIndex oneway - - «create»
   */
  public static abstract class Factory<V extends NumberVector<?, ?>, I extends PreferenceVectorIndex<V>> implements PreferenceVectorIndex.Factory<V, I>, Parameterizable {
    @Override
    public abstract I instantiate(Relation<V> relation);

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }
  }
}