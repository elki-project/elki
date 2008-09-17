package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface defining the requirements for classes
 * using a {@link de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler}
 * to run a preprocessor on a certain database.
 *
 * @author Elke Achtert
 */
public interface PreprocessorClient extends Parameterizable {

    /**
     * Returns the name of the default preprocessor.
     *
     * @return the name of the default preprocessor
     */
    String getDefaultPreprocessorClassName();

    /**
     * Returns the description for the preprocessor parameter.
     *
     * @return the description for the preprocessor parameter
     */
    String getPreprocessorDescription();

    /**
     * Returns the super class for the preprocessor parameter.
     *
     * @return the super class for the preprocessor parameter
     */
    @SuppressWarnings("unchecked")
    Class<? extends Preprocessor> getPreprocessorSuperClass();

    /**
     * Returns the association ID for the association to be set by the preprocessor.
     *
     * @return the association ID for the association to be set by the preprocessor
     */
    AssociationID<?> getAssociationID();
}
