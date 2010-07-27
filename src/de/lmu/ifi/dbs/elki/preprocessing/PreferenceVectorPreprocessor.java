package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;

/**
 * Marker interface for preprocessors computing preference vectors.
 *
 * @param <O> input object type
 * @author Elke Achtert 
 */
public interface PreferenceVectorPreprocessor<O extends DatabaseObject> extends Preprocessor<O, BitSet>, InspectionUtilFrequentlyScanned {
	//TODO any methods??
}
