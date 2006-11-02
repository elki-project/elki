package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.DatabaseObject;

/**
 * Marker interface for preprocessors computing preference vectors.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface PreferenceVectorPreprocessor<O extends DatabaseObject> extends Preprocessor<O> {
}
