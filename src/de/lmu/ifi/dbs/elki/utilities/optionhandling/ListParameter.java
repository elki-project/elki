package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract parameter class defining a parameter for a list of objects.
 *
 * @author Steffi Wanka
 * @param <T>
 */
public abstract class ListParameter<T> extends Parameter<List<T>, List<T>> {

    /**
     * A pattern defining a &quot,&quot.
     */
    public static final Pattern SPLIT = Pattern.compile(",");

    /**
     * A pattern defining a &quot:&quot.
     */
    public static final Pattern VECTOR_SPLIT = Pattern.compile(":");

    /**
     * Constructs a list parameter with the given optionID.
     *
     * @param optionID the unique id of this parameter
     */
    public ListParameter(OptionID optionID) {
        super(optionID);
    }

    /**
     * Constructs a list parameter with the given optionID.
     *
     * @param optionID   the unique id of this parameter
     * @param constraint the constraint of this parameter
     */
    public ListParameter(OptionID optionID, ParameterConstraint<List<T>> constraint) {
        super(optionID, constraint);
    }

    /**
     * Constructs a list parameter with the given optionID.
     *
     * @param optionID     the unique id of this parameter
     * @param constraint   the constraint of this parameter, may be null
     * @param optional     specifies if this parameter is an optional parameter
     * @param defaultValue the default value of this parameter (may be null)
     */
    public ListParameter(OptionID optionID, ParameterConstraint<List<T>> constraint, boolean optional, List<T> defaultValue) {
        super(optionID, constraint, optional, defaultValue);
    }

    /**
     * Returns the size of this list parameter.
     *
     * @return the size of this list parameter.
     */
    public int getListSize() {
        if (this.value == null && isOptional()) {
            return 0;
        }

        return this.value.size();
    }

    /**
     * Returns a string representation of this list parameter. The elements of
     * this list parameters are given in &quot;[ ]&quot;, comma separated.
     */
    public String toString() {
        if (this.value == null) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");

        for (int i = 0; i < this.value.size(); i++) {
            buffer.append(this.value.get(i).toString());
            if (i != this.value.size() - 1) {
                buffer.append(",");
            }
        }
        buffer.append("]");
        return buffer.toString();
    }
}
