package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * Provides an abstract algorithm already setting the distance function.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
public abstract class DistanceBasedAlgorithm<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractAlgorithm<O> {

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "algorithm.distancefunction",
        "Classname of the distance function to determine the distance between database objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + "."
    );

    /**
     * Parameter to specify the distance function to determine the distance between database objects,
     * must extend {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
     * <p>Key: {@code -algorithm.distancefunction} </p>
     * <p>Default value: {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction} </p>
     */
    protected final ClassParameter<DistanceFunction> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<DistanceFunction>(
            DISTANCE_FUNCTION_ID,
            DistanceFunction.class,
            EuclideanDistanceFunction.class.getName());

    /**
     * Holds the instance of the distance function specified by {@link #DISTANCE_FUNCTION_PARAM}.
     */
    private DistanceFunction<O, D> distanceFunction;

    /**
     * Adds parameter
     * {@link #DISTANCE_FUNCTION_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    protected DistanceBasedAlgorithm() {
        super();
        // parameter distance function
        addOption(DISTANCE_FUNCTION_PARAM);
    }

    /**
     * Calls {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and instantiates {@link #distanceFunction} according to the value of parameter
     * {@link #DISTANCE_FUNCTION_PARAM}.
     * The remaining parameters are passed to the {@link #distanceFunction}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // distance function
        // noinspection unchecked
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#getAttributeSettings()}
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #distanceFunction}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(distanceFunction.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Returns the distanceFunction.
     *
     * @return the distanceFunction
     */
    public DistanceFunction<O, D> getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Calls {@link AbstractAlgorithm#parameterDescription()}
     * and appends the parameter description of {@link #distanceFunction} if it is already initialized.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
     */
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());
        if (distanceFunction != null) {
            description.append(distanceFunction.parameterDescription());
        }
        return description.toString();
    }
}
