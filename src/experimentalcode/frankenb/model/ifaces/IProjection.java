package experimentalcode.frankenb.model.ifaces;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * Projects a given Relation
 * 
 * @author Florian Frankenberger
 */
public interface IProjection<V> {
  public Relation<V> project(Relation<V> dataSet) throws UnableToComplyException;
 
}