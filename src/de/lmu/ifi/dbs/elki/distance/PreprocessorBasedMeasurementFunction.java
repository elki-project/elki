package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;

/**
 * Describes the requirements of any measurement
 * function (e.g. distance function or similarity function) needing a preprocessor
 * running on a database.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used as measurement for comparing database objects
 * @param <O> the type of DatabaseObject for which a measurement is provided for comparison
 * @param <P> the type of Preprocessor used
 */
public interface PreprocessorBasedMeasurementFunction<O extends DatabaseObject, P extends Preprocessor<O>, D extends Distance<D>>
    extends MeasurementFunction<O, D> {

    /**
     * Returns the preprocessor of this measurement function.
     *
     * @return the preprocessor of this measurement function
     */
    P getPreprocessor();

    /**
     * Returns the name of the default preprocessor.
     *
     * @return the name of the default preprocessor
     */
    String getDefaultPreprocessorClassName();

    /**
     * Returns the description for the preprocessor.
     *
     * @return the description for the preprocessor
     */
    String getPreprocessorDescription();

    /**
     * Returns the super class for the preprocessor.
     *
     * @return the super class for the preprocessor
     */
    Class<? extends Preprocessor> getPreprocessorSuperClassName();

    /**
     * Returns the assocoiation ID for the association to be set by the preprocessor.
     *
     * @return the assocoiation ID for the association to be set by the preprocessor
     */
    AssociationID getAssociationID();
}
