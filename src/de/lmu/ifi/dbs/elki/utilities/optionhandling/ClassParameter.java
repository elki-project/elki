package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.properties.PropertyName;

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
    @SuppressWarnings("unchecked")
    public ClassParameter(OptionID optionID, Class<?> restrictionClass) {
        // It would be nice to be able to use Class<C> here, but this won't work with
        // nested Generics:
        // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
        // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
        // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an invalid cast.
        super(optionID);
        this.restrictionClass = (Class<C>) restrictionClass;
    }

    /**
     * Constructs a class parameter with the given optionID,
     * restriction class, and optional flag.
     *
     * @param optionID         the unique id of the option
     * @param restrictionClass the restriction class of this class parameter
     * @param optional         specifies if this parameter is an optional parameter
     */
    public ClassParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
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
    public ClassParameter(OptionID optionID, Class<?> restrictionClass, String defaultValue) {
        this(optionID, restrictionClass);
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
            throw new WrongParameterValueException("Parameter Error.\n" +
                "No value for parameter \"" + getName() + "\" " + "given.");
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
            throw new WrongParameterValueException(this.name, value, "subclass / implementing class of "
                + restrictionClass.getName(), e);
        }
        throw new WrongParameterValueException(this.name, value, "subclass / implementing class of "
            + restrictionClass.getName());
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

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;class&gt;&quot;
     */
    @Override
    protected String getParameterType() {
        return "<class>";
    }

    /**
     * Returns a new instance for the value (i.e., the class name)
     * of this class parameter. The instance has the
     * type of the restriction class of this class parameter.
     * <p/> If the Class for the class name is not found, the instantiation is tried
     * using the package of the restriction class as package of the class name.
     *
     * @return a new instance for the value of this class parameter
     * @throws ParameterException if the instantiation cannot be performed successfully
     *                            or the value of this class parameter is not set
     */
    public C instantiateClass() throws ParameterException {
        if (value == null && !optionalParameter) {
            throw new UnusedParameterException("Value of parameter " + name + " has not been specified.");
        }
        C instance;
        try {
            try {
                instance = restrictionClass.cast(Class.forName(value).newInstance());
            }
            catch (ClassNotFoundException e) {
                // try package of type
                instance = restrictionClass.cast(Class.forName(restrictionClass.getPackage().getName() +
                    "." + value).newInstance());
            }
        }
        catch (Exception e) {
            throw new WrongParameterValueException(name, value, getDescription(), e);
        }
        return instance;
    }

}