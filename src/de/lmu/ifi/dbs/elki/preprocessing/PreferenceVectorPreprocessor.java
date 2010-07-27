package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;

/**
 * Marker interface for preprocessors computing preference vectors.
 *
 * @param <O> input object type
 * @author Elke Achtert 
 */
public interface PreferenceVectorPreprocessor<O extends DatabaseObject> extends Preprocessor<O, BitSet>, InspectionUtilFrequentlyScanned {
  /**
   * This method executes the particular preprocessing step of this Preprocessor
   * for the objects of the specified database.
   * 
   * @param database the database for which the preprocessing is performed
   */
  public <T extends O> PreferenceVectorPreprocessor.Instance<T> instantiate(Database<T> database);
  
  /**
   * Instance interface.
   * 
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
	public static interface Instance<O extends DatabaseObject> extends Preprocessor.Instance<BitSet> {
	  // Empty
	}
}