package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.properties.PropertyName;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying a list of class names.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @param <C> Class type
 */
// TODO: Add missing constructors. (ObjectListParameter also!)
public class ClassListParameter<C> extends ListParameter<Class<? extends C>> {
  /**
   * The restriction class for the list of class names.
   */
  protected Class<C> restrictionClass;

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
    super(optionID, optional);
    this.restrictionClass = (Class<C>) restrictionClass;
  }

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

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    StringBuffer buf = new StringBuffer();
    final String defPackage = restrictionClass.getPackage().getName() + ".";
    for (Class<? extends C> c : getValue()) {
      if (buf.length() > 0) {
        buf.append(LIST_SEP);
      }
      String name = c.getName();
      if(name.startsWith(defPackage)) {
        name = name.substring(defPackage.length());
      }      
      buf.append(name);
    }
    return buf.toString();
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected List<Class<? extends C>> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for(Object o : l) {
        if(!(o instanceof Class)) {
          throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given list contains objects of different type!");
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list?
      return (List<Class<? extends C>>) l;
    }
    catch(ClassCastException e) {
      // continue with others
    }
    if(obj instanceof String) {
      String[] classes = SPLIT.split((String) obj);
      // TODO: allow empty lists (and list constraints) to enforce length?
      if(classes.length == 0) {
        throw new UnspecifiedParameterException("Wrong parameter format! Given list of classes for parameter \"" + getName() + "\" is either empty or has the wrong format!");
      }

      List<Class<? extends C>> cls = new ArrayList<Class<? extends C>>(classes.length);
      for(String cl : classes) {
        try {
          Class<?> c;
          try {
            c = Class.forName(cl);
          }
          catch(ClassNotFoundException e) {
            // try in package of restriction class
            c = Class.forName(restrictionClass.getPackage().getName() + "." + cl);
          }
          // Redundant check, also in validate(), but not expensive.
          if(!restrictionClass.isAssignableFrom(c)) {
            throw new WrongParameterValueException(this, cl, "Class \"" + cl + "\" does not extend/implement restriction class " + restrictionClass + ".\n");
          }
          else {
            cls.add((Class<? extends C>) c);
          }
        }
        catch(ClassNotFoundException e) {
          throw new WrongParameterValueException(this, cl, "Class \"" + cl + "\" not found.\n", e);
        }
      }
      return cls;
    }
    // INCOMPLETE
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of Class values!");
  }
  
  /** {@inheritDoc} */
  @Override
  protected boolean validate(List<Class<? extends C>> obj) throws ParameterException {
    for (Class<? extends C> cls : obj) {
      if (!restrictionClass.isAssignableFrom(cls)) {
        throw new WrongParameterValueException(this, cls.getName(), "Class \"" + cls.getName() + "\" does not extend/implement restriction class " + restrictionClass + ".\n");
      }
    }
    return super.validate(obj);
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

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;class_1,...,class_n&gt;&quot;
   */
  @Override
  public String getSyntax() {
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
   * @return a list of new instances for the value of this class list parameter
   * @throws ParameterException if the instantiation cannot be performed
   *         successfully or the value of this class list parameter is not set
   */
  public List<C> instantiateClasses(Parameterization config) {
    config = config.descend(this);
    List<C> instances = new ArrayList<C>();
    if(getValue() == null) {
      config.reportError(new UnusedParameterException("Value of parameter " + getName() + " has not been specified."));
      return instances; // empty list.
    }

    for(Class<? extends C> cls : getValue()) {
      // NOTE: There is a duplication of this code in ObjectListParameter - keep in sync!
      try {
        C instance = ClassGenericsUtil.tryInstanciate(restrictionClass, cls, config);
        instances.add(instance);
      }
      catch(Exception e) {
        config.reportError(new WrongParameterValueException(this, cls.getName(), e));
      }
    }
    return instances;
  }
}