package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction;
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
     * Parameter to specify the distance function to determine the distance between database objects,
     * must extend {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
     * <p>Key: {@code -algorithm.distancefunction} </p>
     * <p>Default value: {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction} </p>
     */
    protected final ClassParameter<DistanceFunction> DISTANCE_FUNCTION_PARAM =
        new ClassParameter<DistanceFunction>(OptionID.ALGORITHM_DISTANCEFUNCTION,
            DistanceFunction.class, EuklideanDistanceFunction.class.getName());

    /**
     * The distance function.
     */
    private DistanceFunction<O, D> distanceFunction;

    /**
     * Adds parameter for distance function to parameter map.
     */
    protected DistanceBasedAlgorithm() {
        super();
        // parameter distance function
        addOption(DISTANCE_FUNCTION_PARAM);
    }

    /**
     * Calls
     * {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and sets additionally the distance function, passing remaining parameters
     * to the set distance function.
     *
     * @see AbstractAlgorithm#setParameters(String[])
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

}
