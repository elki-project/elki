package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

import java.util.List;

/**
 * NormalizationWrapper is an abstract super class for all
 * file based database connection wrappers that need to normalize the input data.
 *
 * @author Elke Achtert
 */
public abstract class NormalizationWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * @see de.lmu.ifi.dbs.elki.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // normalization
        Util.addParameter(parameters, OptionID.NORMALIZATION, AttributeWiseRealVectorNormalization.class.getName());
        Util.addFlag(parameters, OptionID.NORMALIZATION_UNDO);

        return parameters;
    }
}
