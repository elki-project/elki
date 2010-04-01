package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Marker interface for preprocessors computing preference vectors.
 *
 * @author Elke Achtert 
 * @param <O> Object type
 */
public interface PreferenceVectorPreprocessor<O extends DatabaseObject> extends Preprocessor<O, BitSet> {
	//TODO any methods??
}
