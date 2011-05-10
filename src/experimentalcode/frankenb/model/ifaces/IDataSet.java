package experimentalcode.frankenb.model.ifaces;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * This is a simple intermediate state of Data in a DataBase.
 * Image a projection reduces the dimensions used, then the original ids are preserved but
 * the vectors are different. The new vectors would be stored in a <code>IDataSet</code>
 * 
 * @author Florian Frankenberger
 */
public interface IDataSet<V> extends Relation<V> {
}
