package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract super class for distance functions needing a preprocessor.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 * @param <P> the type of Preprocessor used
 */
public abstract class AbstractPreprocessorBasedDistanceFunction<O extends DatabaseObject,
    P extends Preprocessor<O>, D extends Distance<D>> extends AbstractDistanceFunction<O, D> {

    /**
     * The handler class for the preprocessor.
     */
    private final PreprocessorHandler<O, P> preprocessorHandler;

    /**
     * Provides a super class for distance functions needing a preprocessor
     *
     * @param pattern a pattern to define the required input format
     */
    public AbstractPreprocessorBasedDistanceFunction(Pattern pattern) {
        super(pattern);
        preprocessorHandler = new PreprocessorHandler(
            optionHandler,
            this.getPreprocessorClassDescription(),
            this.getPreprocessorSuperClassName(),
            this.getDefaultPreprocessorClassName(),
            this.getAssociationID());
    }

    /**
     * Runs the preprocessor on the database.
     *
     * @param database the database to be set
     * @param verbose  flag to allow verbose messages while performing the method
     * @param time     flag to request output of performance time
     */
    @Override
    public void setDatabase(Database<O> database, boolean verbose, boolean time) {
        super.setDatabase(database, verbose, time);
        preprocessorHandler.runPreprocessor(database, verbose, time);
    }

    /**
     * Sets the values for the parameters delta and preprocessor if specified.
     * If the parameters are not specified default values are set.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        remainingParameters = preprocessorHandler.setParameters(optionHandler, remainingParameters);

        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> result = super.getAttributeSettings();
        preprocessorHandler.addAttributeSettings(result);
        return result;
    }

    /**
     * Returns the preprocessor of this distance function.
     *
     * @return the preprocessor of this distance function
     */
    public P getPreprocessor() {
        return preprocessorHandler.getPreprocessor();
    }

    /**
     * Returns the name of the default preprocessor.
     *
     * @return the name of the default preprocessor
     */
    abstract String getDefaultPreprocessorClassName();

    /**
     * Returns the description for parameter preprocessor.
     *
     * @return the description for parameter preprocessor
     */
    abstract String getPreprocessorClassDescription();

    /**
     * Returns the super class for the preprocessor.
     *
     * @return the super class for the preprocessor
     */
    abstract Class<? extends Preprocessor> getPreprocessorSuperClassName();

    /**
     * Returns the assocoiation ID for the association to be set by the preprocessor.
     *
     * @return the assocoiation ID for the association to be set by the preprocessor
     */
    abstract AssociationID getAssociationID();
}
