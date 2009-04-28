package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Abstract super class for locally weighted distance functions using a preprocessor
 * to compute the local weight matrix.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <P> preprocessor type
 */
public abstract class AbstractLocallyWeightedDistanceFunction<O extends RealVector<O, ?>, P extends Preprocessor<O>>
    extends AbstractDoubleDistanceFunction<O> implements PreprocessorClient {

    /**
     * The handler class for the preprocessor.
     */
    private final PreprocessorHandler<O, P> preprocessorHandler;

    /**
     * Provides an abstract locally weighted distance function.
     */
    protected AbstractLocallyWeightedDistanceFunction() {
        super();
        preprocessorHandler = new PreprocessorHandler<O, P>(this);
    }

    @Override
    public void setDatabase(Database<O> database, boolean verbose, boolean time) {
        super.setDatabase(database, verbose, time);
        preprocessorHandler.runPreprocessor(database, verbose, time);
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(String[])
     * AbstractParameterizable#setParameters(args)}
     * and passes the remaining parameters to the {@link #preprocessorHandler}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        remainingParameters = preprocessorHandler.setParameters(remainingParameters);
        addParameterizable(preprocessorHandler);
        
        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        description.append(optionHandler.usage("Locally weighted distance function. Pattern for defining a range: \"" +
            requiredInputPattern() + "\".", false));
        description.append("\nRequires parametrization of underlying preprocessor handler:\n");
        description.append(preprocessorHandler.parameterDescription());
        return description.toString();
    }

    /**
     * @return the name of the default preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return KnnQueryBasedHiCOPreprocessor.class.getName();
    }

    public String getPreprocessorDescription() {
        return "Preprocessor class to determine the correlation dimension of each object.";
    }


    /**
     * @return the super class for the preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return Preprocessor.class;
    }
}
