package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

/**
 * Abstract superclass for specifying program arguments.
 *
 * @author Steffi Wanka
 * @param <T> the type of a possible value for this option
 */
public abstract class Option<T> extends AbstractLoggable {

    /**
     * The option name.
     */
    protected final String name;

    /**
     * The short description of the optionn. An extended description
     * is provided by the method {@link #getDescription()}
     */
    protected String shortDescription;

    /**
     * The value of this option.
     */
    protected T value;

    /**
     * Creates an option which is guaranteed
     * to be have an unique name.
     *
     * @param optionID the unique id of the option
     */
    public Option(OptionID optionID) {
        super(LoggingConfiguration.DEBUG);
        this.name = optionID.getName();
        this.shortDescription = optionID.getDescription();
    }

    /**
     * Sets the name and description of the option.
     *
     * @param name        The name of the option.
     * @param description The description of the option.
     * @deprecated use Option(optionID) instead
     */
    @Deprecated
    public Option(String name, String description) {
        super(LoggingConfiguration.DEBUG);
        this.name = name;
        this.shortDescription = description;
    }

    /**
     * Returns the name of the option.
     *
     * @return the option's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the short description of the option.
     *
     * @return the option's short description.
     */
    protected String getShortDescription() {
        return shortDescription;
    }

    /**
     * Sets the short description of the option.
     * @param description the short description to be set
     */
    public void setShortDescription(String description) {
        this.shortDescription = description;
    }

    /**
     * Returns the extended description of the option
     * which includes the option's type, the short description
     * and the default value (if specified).
     *
     * @return the option's description.
     */
    public abstract String getDescription();

    /**
     * Returns true if the value of the option is set, false otherwise.
     *
     * @return true if the value of the option is set, false otherwise.
     */
    public abstract boolean isSet();

    /**
     * Sets the value of the option.
     *
     * @param value the option's value to be set
     * @throws ParameterException if the given value is not a valid value for this option.
     */
    public abstract void setValue(String value) throws ParameterException;

    /**
     * Returns the value of the option.
     *
     * @return the option's value.
     * @throws UnusedParameterException is not thrown actually in this class
     *                                  but subclasses may require allowance to throw this Exception
     */
    public T getValue() throws UnusedParameterException {
        return this.value;
    }

    /**
     * Checks if the given argument is valid for this option.
     *
     * @param value option value to be checked
     * @return true, if the given value is valid for this option
     * @throws ParameterException if the given value is not a valid value for this option.
     */
    public abstract boolean isValid(String value) throws ParameterException;
}
