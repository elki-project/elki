package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter that represents a list of objects (in contrast to a class list parameter, they will be instanced at most once.)
 * 
 * @author Erich Schubert
 *
 * @param <C> Class type
 */
// TODO: add missing constructors.
public class ObjectListParameter<C> extends ClassListParameter<C> {
  /**
   * Cache for the generated instances.
   */
  private ArrayList<C> instances = null;
  
  /**
   * Constructor with optional flag.
   * 
   * @param optionID Option ID
   * @param restrictionClass Restriction class
   * @param optional optional flag
   */
  public ObjectListParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, restrictionClass, optional);
  }

  /**
   * Constructor for non-optional.
   * 
   * @param optionID Option ID
   * @param restrictionClass Restriction class
   */
  public ObjectListParameter(OptionID optionID, Class<?> restrictionClass) {
    super(optionID, restrictionClass);
  }
  
  /** {@inheritDoc} */
  @Override
  public String getSyntax() {
    return "<object_1|class_1,...,object_n|class_n>";
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected List<Class<? extends C>> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
    }
    if (List.class.isInstance(obj)) {
      List<?> l = (List<?>) obj;
      ArrayList<C> inst = new ArrayList<C>(l.size());
      ArrayList<Class<? extends C>> classes = new ArrayList<Class<? extends C>>(l.size());
      for (Object o : l) {
        // does the given objects class fit?
        if (restrictionClass.isInstance(o)) {
          inst.add((C) o);
          classes.add((Class<? extends C>) o.getClass());
        } else if (o instanceof Class) {
          if (restrictionClass.isAssignableFrom((Class<?>)o)) {
          inst.add(null);
          classes.add((Class<? extends C>) o);
          } else {
            throw new WrongParameterValueException(this, ((Class<?>)o).getName(), "Given class not a subclass / implementation of " + restrictionClass.getName());
          }
        } else {
          throw new WrongParameterValueException(this, o.getClass().getName(), "Given instance not an implementation of " + restrictionClass.getName());
        }
      }
      this.instances = inst;
      return super.parseValue(classes);
    }
    return super.parseValue(obj);
  }

  /** {@inheritDoc} */
  @Override
  public List<C> instantiateClasses(Parameterization config) {
    if (instances == null) {
      // instantiateClasses will descend itself.
      instances = new ArrayList<C>(super.instantiateClasses(config));
    } else {
      Parameterization cfg = null;
      for (int i = 0; i < instances.size(); i++) {
        if (instances.get(i) == null) {
          Class<? extends C> cls = getValue().get(i);
          try {
            // Descend at most once, and only when needed
            if (cfg == null) {
              cfg = config.descend(this);
            }
            C instance = ClassGenericsUtil.tryInstanciate(restrictionClass, cls, cfg);
            instances.set(i, instance);
          }
          catch(Exception e) {
            config.reportError(new WrongParameterValueException(this, cls.getName(), e));
          }
        }
      }
    }
    return new ArrayList<C>(instances);
  } 
}