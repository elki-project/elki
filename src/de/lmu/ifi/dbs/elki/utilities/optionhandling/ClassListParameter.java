package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.properties.PropertyName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parameter class for a parameter specifying a list of class names.
 * 
 * @author Steffi Wanka
 * @param <C> Class type
 */
public class ClassListParameter<C> extends ListParameter<String> {

  /**
   * The restriction class for the list of class names.
   */
  private Class<C> restrictionClass;

  /**
   * Constructs a class list parameter with the given optionID and restriction
   * class.
   * 
   * @param optionID the unique id of this parameter
   * @param restrictionClass the restriction class of the list of class names
   */
  @SuppressWarnings("unchecked")
  public ClassListParameter(OptionID optionID, Class<?> restrictionClass) {
    super(optionID);
    this.restrictionClass = (Class<C>) restrictionClass;
  }

  /**
   * Constructs a class list parameter with the given optionID and restriction
   * class.
   * 
   * @param optionID the unique id of this parameter
   * @param restrictionClass the restriction class of the list of class names
   * @param optional specifies if this parameter is an optional parameter
   */
  @SuppressWarnings("unchecked")
  public ClassListParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, null, optional, null);
    this.restrictionClass = (Class<C>) restrictionClass;
  }

  @Override
  public void setValue(String value) throws ParameterException {
    if(isValid(value)) {
      String[] classes = SPLIT.split(value);
      this.value = Arrays.asList(classes);
    }
  }

  /**
   * Returns the class names allowed according to the restriction class of this
   * parameter.
   * 
   * @return class names allowed according to the restriction class defined.
   */
  public String[] getRestrictionClasses() {
    if(restrictionClass != null) {
      return Properties.ELKI_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(restrictionClass));
    }
    return new String[] {};
  }

  @Override
  public boolean isValid(String value) throws ParameterException {
    String[] classes = SPLIT.split(value);
    if(classes.length == 0) {
      throw new WrongParameterValueException(this.name, value, "Wrong parameter format! Given list of classes for paramter \"" + getName() + "\" is either empty or has the wrong format!\nParameter value required:\n" + getDescription());
    }

    for(String cl : classes) {
      try {
        if(!ClassParameter.satisfiesClassRestriction(restrictionClass, cl)) {
          throw new WrongParameterValueException(this.name, cl, "Wrong parameter value for parameter \"" + getName() + "\". Given class " + cl + " does not extend restriction class " + restrictionClass + ".\n");
        }
      }
      catch(ClassNotFoundException e) {
        throw new WrongParameterValueException(this.name, cl, "Wrong parameter value for parameter \"" + getName() + "\". Given class " + cl + " not found.\n", e);
      }
    }

    return true;
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;class_1,...,class_n&gt;&quot;
   */
  @Override
  protected String getParameterType() {
    return "<class_1,...,class_n>";
  }

  /**
   * Returns a list of new instances for the value (i.e., the class name) of
   * this class list parameter. The instances have the type of the restriction
   * class of this class list parameter.
   * <p/>
   * If the Class for the class names is not found, the instantiation is tried
   * using the package of the restriction class as package of the class name.
   * 
   * @return alist of new instances for the value of this class list parameter
   * @throws ParameterException if the instantiation cannot be performed
   *         successfully or the value of this class list parameter is not set
   */
  public List<C> instantiateClasses() throws ParameterException {
    if(value == null && !optionalParameter) {
      throw new UnusedParameterException("Value of parameter " + name + " has not been specified.");
    }
    List<C> instances = new ArrayList<C>();

    for(String classname : value) {
      try {
        try {
          instances.add(restrictionClass.cast(Class.forName(classname).newInstance()));
        }
        catch(ClassNotFoundException e) {
          // try package of type
          instances.add(restrictionClass.cast(Class.forName(restrictionClass.getPackage().getName() + "." + classname).newInstance()));
        }
      }
      catch(Exception e) {
        throw new WrongParameterValueException(name, classname, getDescription(), e);
      }
    }

    return instances;
  }

}
