package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;

/**
 * Parameter class for a parameter specifying a class name.
 *
 * @author Steffi Wanka
 */
public class ClassParameter<C> extends Parameter<String, String> {

    /**
     * The restriction class for this class parameter.
     */
    private Class<C> restrictionClass;

    /**
     * Constructs a class parameter with the given optionID, and
     * restriction class.
     *
     * @param optionID         the unique id of the option
     * @param restrictionClass the restriction class of this class parameter
     */
    public ClassParameter(OptionID optionID, Class<C> restrictionClass) {
        super(optionID);
        this.restrictionClass = restrictionClass;
    }

    /**
     * Constructs a class parameter with the given optionID,
     * restriction class, and optional flag.
     *
     * @param optionID         the unique id of the option
     * @param restrictionClass the restriction class of this class parameter
     * @param optional         specifies if this parameter is an optional parameter
     */
    public ClassParameter(OptionID optionID, Class<C> restrictionClass, boolean optional) {
        this(optionID, restrictionClass);
        setOptional(optional);
    }

    /**
     * Constructs a class parameter with the given optionID,
     * restriction class, and default value.
     *
     * @param optionID         the unique id of the option
     * @param restrictionClass the restriction class of this class parameter
     * @param defaultValue     the default value of this class parameter
     */
    public ClassParameter(OptionID optionID, Class<C> restrictionClass, String defaultValue) {
        this(optionID, restrictionClass);
        setDefaultValue(defaultValue);
    }

    /**
     * Constructs a class parameter with the given name, description, and
     * restriction class.
     *
     * @param name             the parameter name
     * @param description      the parameter description
     * @param restrictionClass the restriction class of this class parameter
     * @deprecated
     */
    public ClassParameter(String name, String description, Class<C> restrictionClass) {
        super(name, description);
        this.restrictionClass = restrictionClass;
    }

    /**
     * Constructs a class parameter with the given name, description,
     * restriction class, and default value.
     *
     * @param name             the parameter name
     * @param description      the parameter description
     * @param restrictionClass the restriction class of this class parameter
     * @param defaultValue     the default value of this class parameter
     * @deprecated
     */
    public ClassParameter(String name, String description, Class<C> restrictionClass, String defaultValue) {
        this(name, description, restrictionClass);
        setDefaultValue(defaultValue);
    }

    @Override
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            setCorrectValue(value);
        }
    }

    /**
     * Returns the class names allowed according to the restriction class of
     * this class parameter.
     *
     * @return class names allowed according to the restriction class defined.
     */
    public String[] getRestrictionClasses() {
        if (restrictionClass != null) {
            return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(restrictionClass));
        }
        return new String[]{};
    }

    /**
     * Checks if the given parameter value is valid for this ClassParameter. If
     * not a parameter exception is thrown.
     */
    @Override
    public boolean isValid(String value) throws ParameterException {
        if (value == null) {
            throw new WrongParameterValueException("Parameter Error.\nNo value for parameter \"" + getName() + "\" " + "given.");
        }

        try {
            try {
                if (restrictionClass.isAssignableFrom(Class.forName(value))) {
                    return true;
                }
            }

            catch (ClassNotFoundException e) {
                restrictionClass.isAssignableFrom(Class.forName(restrictionClass.getPackage().getName() + "." + value));
                return true;
            }
        }

        catch (ClassNotFoundException e) {
            throw new WrongParameterValueException(this.name, value, "", e);
        }
        throw new WrongParameterValueException(this.name, value, "subclass of " + restrictionClass.getName());
    }

    /**
     * Returns the restriction class of this class parameter.
     *
     * @return the restriction class of this class parameter.
     */
    public Class<C> getRestrictionClass() {
        return restrictionClass;
    }

    /**
     * Sets the restriction class of
     * this class parameter.
     *
     * @param restrictionClass the restriction class to be set
     */
    public void setRestrictionClass(Class<C> restrictionClass) {
        this.restrictionClass = restrictionClass;
    }

    /**
     * Tries to set the correct value for this class parameter.
     *
     * @param value the value to be set
     * @throws ParameterException if the specified value is not correct (e.g., it is
     *                            not conform with the restriction class)
     */
    private void setCorrectValue(String value) throws ParameterException {
        try {
            try {
                if (restrictionClass.isAssignableFrom(Class.forName(value))) {
                    this.value = value;
                }
            }

            catch (ClassNotFoundException e) {
                restrictionClass.isAssignableFrom(Class.forName(restrictionClass.getPackage().getName() + "." + value));
                this.value = restrictionClass.getPackage().getName() + "." + value;
            }
        }
        catch (ClassNotFoundException e) {
            throw new WrongParameterValueException(this.name, value, "subclass of " + restrictionClass.getName());
        }
    }
}