package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;

/**
 * Wrapper class for the dependency derivator.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class DependencyDerivatorWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new DependencyDerivatorWrapper<DatabaseObject>().runCLIWrapper(args);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm DependencyDerivator
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, DependencyDerivator.class.getName());

        // normalization
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

        return parameters;
    }
}
