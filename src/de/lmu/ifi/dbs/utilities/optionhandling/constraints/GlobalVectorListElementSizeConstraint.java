package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.VectorListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Global parameter constraint for testing if the dimensions of each vector specified by a
 * given vector list parameter ({@link VectorListParameter})
 * correspond to the value of a integer parameter ({@link IntParameter}) given.
 *
 * @author Steffi Wanka
 */
public class GlobalVectorListElementSizeConstraint extends AbstractLoggable implements GlobalParameterConstraint {

    /**
     * Vector list parameter.
     */
    private VectorListParameter vector;

    /**
     * Integer parameter providing the size constraint.
     */
    private IntParameter size;

    /**
     * Constructs a global vector size constraint.
     * <p/>
     * Each vector of the vector list parameter given is tested for being equal
     * to the value of the integer parameter given.
     *
     * @param vector         the vector list parameter
     * @param sizeConstraint the integer parameter providing the size constraint
     */
    public GlobalVectorListElementSizeConstraint(VectorListParameter vector, IntParameter sizeConstraint) {
        super(LoggingConfiguration.DEBUG);
        this.vector = vector;
        this.size = sizeConstraint;
    }

    /**
     * Checks if the dimensions of each vector of the vector list parameter have
     * the appropriate size provided by the integer parameter. If not, a
     * parameter exception will be thrown.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
     */
    public void test() throws ParameterException {
        if (!vector.isSet())
            return;

        for (List<Double> vec : vector.getValue()) {
            if (vec.size() != size.getValue()) {
                throw new WrongParameterValueException("Global Parameter Constraint Error.\n" +
                    "The vectors of vector list parameter " + vector.getName() +
                    " have not the required dimension of " + size.getValue() +
                    " given by integer parameter " + size.getName() + ".");
            }
        }
    }

    /**
     * @see GlobalParameterConstraint#getDescription()
     */
    public String getDescription() {
        return "The dimensionality of the vectors of vector list parameter " + vector.getName() +
            " must have the value of integer parameter " + size.getName();
    }

}
