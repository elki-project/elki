package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.lang.reflect.InvocationTargetException;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.properties.IterateKnownImplementations;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.properties.PropertyName;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.IterableIteratorAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Parameter class for a parameter specifying a class name.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @param <C> Class type
 */
// TODO: add additional constructors with parameter constraints.
// TODO: turn restrictionClass into a constraint?
public class ClassParameter<C> extends Parameter<Class<?>, Class<? extends C>> {
  /**
   * The restriction class for this class parameter.
   */
  protected Class<C> restrictionClass;

  /**
   * Non-breaking unicode space character.
   */
  public static final String NONBREAKING_SPACE = "\u00a0";

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default value of this class parameter
   */
  @SuppressWarnings("unchecked")
  public ClassParameter(OptionID optionID, Class<?> restrictionClass, Class<?> defaultValue) {
    super(optionID, (Class<? extends C>) defaultValue);
    // It would be nice to be able to use Class<C> here, but this won't work
    // with nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
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
  @SuppressWarnings("unchecked")
  public ClassParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, optional);
    // It would be nice to be able to use Class<C> here, but this won't work
    // with nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
    this.restrictionClass = (Class<C>) restrictionClass;
  }

  /**
   * Constructs a class parameter with the given optionID, and restriction
   * class.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   */
  public ClassParameter(OptionID optionID, Class<?> restrictionClass) {
    this(optionID, restrictionClass, false);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected Class<? extends C> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
    }
    if(obj instanceof Class<?>) {
      return (Class<? extends C>) obj;
    }
    if(obj instanceof String) {
      String value = (String) obj;
      try {
        try {
          return (Class<? extends C>) Class.forName(value);
        }
        catch(ClassNotFoundException e) {
          // Retry with guessed name prefix:
          // restrictionClass.getPackage().getName()
          return (Class<? extends C>) Class.forName(restrictionClass.getPackage().getName() + "." + value);
        }
      }
      catch(ClassNotFoundException e) {
        throw new WrongParameterValueException(this, value, "Given class \"" + value + "\" not found.", e);
      }
    }
    throw new WrongParameterValueException(this, obj.toString(), "Class not found for given value. Must be a subclass / implementation of " + restrictionClass.getName());
  }

  /**
   * Checks if the given parameter value is valid for this ClassParameter. If
   * not a parameter exception is thrown.
   */
  @Override
  public boolean validate(Class<? extends C> obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
    }
    if(!restrictionClass.isAssignableFrom(obj)) {
      throw new WrongParameterValueException(this, obj.getName(), "Given class not a subclass / implementation of " + restrictionClass.getName());
    }
    if(!super.validate(obj)) {
      return false;
    }
    return true;
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
   * This class sometimes provides a list of value descriptions.
   * 
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter#hasValuesDescription()
   */
  @Override
  public boolean hasValuesDescription() {
    return true;
  }

  /**
   * Return a description of known valid classes.
   * 
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter#getValuesDescription()
   */
  @Override
  public String getValuesDescription() {
    if(restrictionClass != null && restrictionClass != Object.class) {
      return restrictionString();
    }
    return "";
  }

  @Override
  public String getValueAsString() {
    String name = getValue().getCanonicalName();
    final String defPackage = restrictionClass.getPackage().getName() + ".";
    if(name.startsWith(defPackage)) {
      return name.substring(defPackage.length());
    }
    return name;
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
  public C instantiateClass(Parameterization config) {
    config = config.descend(this);
    try {
      if(getValue() == null /* && !optionalParameter */) {
        throw new UnusedParameterException("Value of parameter " + getName() + " has not been specified.");
      }
      C instance;
      try {
        instance = ClassGenericsUtil.tryInstanciate(restrictionClass, getValue(), config);
      }
      catch(InvocationTargetException e) {
        // inner exception during instantiation. Log, so we don't lose it!
        LoggingUtil.exception(e);
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class.", e);
      }
      catch(Exception e) {
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class.", e);
      }
      return instance;
    }
    catch(ParameterException e) {
      config.reportError(e);
      return null;
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
   * Get an iterator over all known implementations of the class restriction.
   * 
   * @return {@link java.lang.Iterable Iterable} and {@link java.util.Iterator
   *         Iterator} object
   */
  public IterableIterator<Class<?>> getKnownImplementations() {
    if(InspectionUtil.NONSTATIC_CLASSPATH) {
      return new IterableIteratorAdapter<Class<?>>(InspectionUtil.findAllImplementations(getRestrictionClass(), false));
    }
    return new IterateKnownImplementations(getRestrictionClass());
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
   * Provides a description string listing all classes for the given superclass
   * or interface as specified in the properties.
   * 
   * @param superclass the class to be extended or interface to be implemented
   * @return a description string listing all classes for the given superclass
   *         or interface as specified in the properties
   */
  public String restrictionString() {
    String prefix = restrictionClass.getPackage().getName() + ".";
    StringBuilder info = new StringBuilder();
    if(restrictionClass.isInterface()) {
      info.append("Implementing ");
    }
    else {
      info.append("Extending ");
    }
    info.append(restrictionClass.getName());
    info.append(FormatUtil.NEWLINE);

    IterableIterator<Class<?>> known = getKnownImplementations();
    if(known.hasNext()) {
      info.append("Known classes (default package " + prefix + "):");
      info.append(FormatUtil.NEWLINE);
      for(Class<?> c : known) {
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