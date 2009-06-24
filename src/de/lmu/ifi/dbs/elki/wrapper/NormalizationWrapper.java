package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.normalization.AttributeWiseMinMaxNormalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * NormalizationWrapper is an abstract super class for all
 * file based database connection wrappers that need to normalize the input data.
 * NormalizationWrapper performs an attribute wise normalization on the database objects
 * and reverts the result to the original values.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public abstract class NormalizationWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // normalization
        OptionUtil.addParameter(parameters, OptionID.NORMALIZATION, AttributeWiseMinMaxNormalization.class.getName());
        OptionUtil.addFlag(parameters, OptionID.NORMALIZATION_UNDO);

        return parameters;
    }
}
