package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Factory interface for indexes.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory,interface
 * @apiviz.uses Index oneway - - «create»
 *
 * @param <V> Input object type
 * @param <I> Index type
 */
public interface IndexFactory<V, I extends Index<?>> extends Parameterizable {
  /**
   * Sets the database in the distance function of this index (if existing).
   * 
   * @param relation the relation to index
   */
  public I instantiate(Relation<V> relation);

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  public TypeInformation getInputTypeRestriction();
}