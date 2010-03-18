package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter representing a single object.
 * 
 * It can be parameterized by giving a class name or class to instantiate, or an existing instance.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @param <C> Class type
 */
public class ObjectParameter<C> extends ClassParameter<C> {
  /**
   * The instance to use.
   */
  private C instance;

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default value of this class parameter
   */
  public ObjectParameter(OptionID optionID, Class<?> restrictionClass, Class<?> defaultValue) {
    super(optionID, restrictionClass, defaultValue);
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default instance of this class parameter
   */
  public <T extends C> ObjectParameter(OptionID optionID, Class<?> restrictionClass, T defaultValue) {
    super(optionID, restrictionClass);
    this.instance = defaultValue;
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and optional flag.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public ObjectParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, restrictionClass, optional);
  }

  /**
   * Constructs a class parameter with the given optionID, and restriction
   * class.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   */
  public ObjectParameter(OptionID optionID, Class<?> restrictionClass) {
    // It would be nice to be able to use Class<C> here, but this won't work
    // with nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
    super(optionID, restrictionClass);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected Class<? extends C> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
    }
    // does the given objects class fit?
    if (restrictionClass.isInstance(obj)) {
      return (Class<? extends C>) obj.getClass();
    }
    return super.parseValue(obj);
  }
  
  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  public void setValue(Object obj) throws ParameterException {
    // This is a bit hackish. But when given an appropriate instance, keep it.
    if (restrictionClass.isInstance(obj)) {
      instance = (C) obj;
    }
    super.setValue(obj);
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;class&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<class|object>";
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
  // TODO: throw
  @Override
  public C instantiateClass(Parameterization config) {
    if (instance != null) {
      return instance;
    }
    // NOTE: instance may be null here, when instantiateClass failed.
    return instance = super.instantiateClass(config);
  }
}