package de.lmu.ifi.dbs.elki.utilities.optionhandling;


import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.properties.IterateKnownImplementations;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.properties.PropertyName;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Parameter class for a parameter specifying a class name.
 * 
 * @author Steffi Wanka
 * @param <C> Class type
 */
public class ClassParameter<C> extends Parameter<String, String> {
  /**
   * Logger
   */
  protected static Logging logger = Logging.getLogger(ClassParameter.class);

  /**
   * The restriction class for this class parameter.
   */
  private Class<C> restrictionClass;
  /**
   * Non-breaking unicode space character.
   */
  public static final String NONBREAKING_SPACE = "\u00a0";

  /**
   * Constructs a class parameter with the given optionID, and restriction
   * class.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   */
  @SuppressWarnings("unchecked")
  public ClassParameter(OptionID optionID, Class<?> restrictionClass) {
    // It would be nice to be able to use Class<C> here, but this won't work
    // with
    // nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
    super(optionID);
    this.restrictionClass = (Class<C>) restrictionClass;
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and optional flag.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public ClassParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    this(optionID, restrictionClass);
    setOptional(optional);
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default value of this class parameter
   */
  public ClassParameter(OptionID optionID, Class<?> restrictionClass, String defaultValue) {
    this(optionID, restrictionClass);
    setDefaultValue(defaultValue);
  }

  @Override
  public void setValue(String value) throws ParameterException {
    if(isValid(value)) {
      setCorrectValue(value);
    }
  }

  /**
   * Returns the class names allowed according to the restriction class of this
   * class parameter.
   * 
   * @return class names allowed according to the restriction class defined.
   */
  public String[] getRestrictionClasses() {
    if(restrictionClass != null) {
      return Properties.ELKI_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(restrictionClass));
    }
    return new String[] {};
  }

  /**
   * Checks if the given parameter value is valid for this ClassParameter. If
   * not a parameter exception is thrown.
   */
  @Override
  public boolean isValid(String value) throws ParameterException {
    if(value == null) {
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
    }
    boolean does = false;
    try {
      does = satisfiesClassRestriction(restrictionClass, value);
    }
    catch(ClassNotFoundException e) {
      throw new WrongParameterValueException(this.getName(), value, "Class not found. Expected subclass / implementing class of " + restrictionClass.getName(), e);
    }
    if(!does) {
      throw new WrongParameterValueException(this.getName(), value, "Class needs to be subclass / implementing class of " + restrictionClass.getName());
    }
    return does;
  }

  /**
   * Try to satisfy a single class restriction
   * 
   * @param restrictionClass
   * @param value
   * @return if the class restriction is satisfied.
   * @throws ClassNotFoundException
   */
  public static boolean satisfiesClassRestriction(Class<?> restrictionClass, String value) throws ClassNotFoundException {
    try {
      if(restrictionClass.isAssignableFrom(Class.forName(value))) {
        return true;
      }
      return false;
    }
    catch(ClassNotFoundException e) {
      // Retry with guessed name prefix: restrictionClass.getPackage().getName()
      if(restrictionClass.isAssignableFrom(Class.forName(restrictionClass.getPackage().getName() + "." + value))) {
        return true;
      }
      return false;
    }
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
   * @throws ParameterException if the specified value is not correct (e.g., it
   *         is not conform with the restriction class)
   */
  private void setCorrectValue(String value) throws ParameterException {
    try {
      try {
        if(restrictionClass.isAssignableFrom(Class.forName(value))) {
          this.value = value;
        }
      }

      catch(ClassNotFoundException e) {
        restrictionClass.isAssignableFrom(Class.forName(restrictionClass.getPackage().getName() + "." + value));
        this.value = restrictionClass.getPackage().getName() + "." + value;
      }
    }
    catch(ClassNotFoundException e) {
      throw new WrongParameterValueException(this.getName(), value, "subclass of " + restrictionClass.getName());
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;class&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<class>";
  }

  /**
   * Returns a new instance for the value (i.e., the class name) of this class
   * parameter. The instance has the type of the restriction class of this class
   * parameter.
   * <p/>
   * If the Class for the class name is not found, the instantiation is tried
   * using the package of the restriction class as package of the class name.
   * 
   * @return a new instance for the value of this class parameter
   * @throws ParameterException if the instantiation cannot be performed
   *         successfully or the value of this class parameter is not set
   */
  public C instantiateClass() throws ParameterException {
    if(value == null && !optionalParameter) {
      throw new UnusedParameterException("Value of parameter " + getName() + " has not been specified.");
    }
    C instance;
    try {
      try {
        instance = restrictionClass.cast(Class.forName(value).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        instance = restrictionClass.cast(Class.forName(restrictionClass.getPackage().getName() + "." + value).newInstance());
      }
    }
    catch(Exception e) {
      throw new WrongParameterValueException(getName(), value, getFullDescription(), e);
    }
    return instance;
  }

  /**
   * This class sometimes provides a list of value descriptions.
   * 
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter#hasValuesDescription()
   */
  @Override
  public boolean hasValuesDescription() {
    return true;
  }

  /**
   * Return a description of known valid classes.
   * 
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter#getValuesDescription()
   */
  @Override
  public String getValuesDescription() {
    return restrictionString(getRestrictionClass());
  }
  
  /**
   * Get an iterator over all known implementations of the class restriction.
   * 
   * @return {@link java.lang.Iterable Iterable} and {@link java.util.Iterator Iterator} object
   */
  public IterateKnownImplementations getKnownImplementations() {
    return new IterateKnownImplementations(getRestrictionClass());
  }
  
  /**
   * Provides a description string listing all classes for the given superclass
   * or interface as specified in the properties.
   * 
   * @param superclass the class to be extended or interface to be implemented
   * @return a description string listing all classes for the given superclass
   *         or interface as specified in the properties
   */
  public String restrictionString(Class<?> superclass) {
    String prefix = superclass.getPackage().getName() + ".";
    StringBuilder info = new StringBuilder();
    if(superclass.isInterface()) {
      info.append("Implementing ");
    }
    else {
      info.append("Extending ");
    }
    info.append(superclass.getName());
    IterateKnownImplementations known = getKnownImplementations();
    if (known.hasNext()) {
      info.append(FormatUtil.NEWLINE);
      info.append("Known classes (default package " + prefix + "):");
      info.append(FormatUtil.NEWLINE);
      for (Class<?> c : known) {
        info.append("->" + NONBREAKING_SPACE);
        String name = c.getName();
        if(name.startsWith(prefix)) {
          info.append(name.substring(prefix.length()));
        }
        else {
          info.append(name);
        }
        info.append(FormatUtil.NEWLINE);
      }
    }
    return info.toString();
  }
}