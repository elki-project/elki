package de.lmu.ifi.dbs.elki.index.preprocessed.preference;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
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
  public BitSet getPreferenceVector(DBID objid) {
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