package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.PreprocessorBasedMeasurementFunction;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Abstract super class for distance functions needing a preprocessor.
 *
 * @author Elke Achtert
 * @param <O> object type
 * @param <P> preprocessor type
 * @param <D> distance type 
 */
public abstract class AbstractPreprocessorBasedSimilarityFunction<O extends DatabaseObject, P extends Preprocessor<O>, D extends Distance<D>>
    extends AbstractSimilarityFunction<O, D> implements PreprocessorBasedMeasurementFunction<O, P, D> {

    /**
     * The handler class for the preprocessor.
     */
    private final PreprocessorHandler<O, P> preprocessorHandler;

    /**
     * Provides a super class for distance functions needing a preprocessor
     *
     * @param pattern a pattern to define the required input format
     */
    public AbstractPreprocessorBasedSimilarityFunction(Pattern pattern) {
        super(pattern);
        preprocessorHandler = new PreprocessorHandler<O, P>(this);
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction#setDatabase(de.lmu.ifi.dbs.elki.database.Database,boolean,boolean)
     * AbstractMeasurementFunction(database, verbose, time)} and
     * runs the preprocessor on the database.
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
     * Calls the super method
     * and passes the remaining parameters to the {@link #preprocessorHandler}.
     *
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        remainingParameters = preprocessorHandler.setParameters(remainingParameters);
        addParameterizable(preprocessorHandler);
        
        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Calls the super method
     * and appends the parameter description of the {@link #preprocessorHandler}.
     *
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // preprocessor handler
        description.append(Description.NEWLINE);
        description.append("Requires parametrization of the underlying preprocessor handler: \n");
        description.append(preprocessorHandler.parameterDescription());

        return description.toString();
    }

    /**
     * @see de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler#getPreprocessor()
     */
    public final P getPreprocessor() {
        return preprocessorHandler.getPreprocessor();
    }
}
