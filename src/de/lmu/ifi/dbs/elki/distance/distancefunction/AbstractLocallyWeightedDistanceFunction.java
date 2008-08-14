package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * Abstract super class for locally weighted distance functions using a preprocessor
 * to compute the local weight matrix.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
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

    /**
     * @see de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction#setDatabase(de.lmu.ifi.dbs.elki.database.Database,boolean,boolean)
     */
    public void setDatabase(Database<O> database, boolean verbose, boolean time) {
        super.setDatabase(database, verbose, time);
        preprocessorHandler.runPreprocessor(database, verbose, time);
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(String[])
     * AbstractParameterizable#setParameters(args)}
     * and passes the remaining parameters to the {@link #preprocessorHandler}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        remainingParameters = preprocessorHandler.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#getAttributeSettings()}
     * and adds to the returned attribute settings the attribute settings of the
     * {@link #preprocessorHandler}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> mySettings = super.getAttributeSettings();
        mySettings.addAll(preprocessorHandler.getAttributeSettings());
        return mySettings;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
     */
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
     * Returns the name of the default preprocessor.
     *
     * @return the name of the default preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor}
     * @see de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient#getDefaultPreprocessorClassName()
     */
    public String getDefaultPreprocessorClassName() {
        return KnnQueryBasedHiCOPreprocessor.class.getName();
    }

    /**
     * @see de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient#getPreprocessorDescription()
     */
    public String getPreprocessorDescription() {
        return "Classname of the preprocessor to determine the correlation dimension of each objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(getPreprocessorSuperClass()) +
            ".";
    }


    /**
     * Returns the super class for the preprocessor.
     *
     * @return the super class for the preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
     * @see de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient#getPreprocessorSuperClass()
     */
    public Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return Preprocessor.class;
    }
}
