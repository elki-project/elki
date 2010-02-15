package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Global parameter constraint describing the dependency of a parameter (
 * {@link Parameter}) on a given flag ({@link Flag}). Depending on the status of
 * the flag the parameter is tested for keeping its constraints or not.
 * 
 * @author Steffi Wanka
 * @param <C> Constraint type
 * @param <S> Parameter type
 */
public class ParameterFlagGlobalConstraint<S, C extends S> implements GlobalParameterConstraint {
  /**
   * Parameter possibly to be checked.
   */
  private Parameter<S,C> param;

  /**
   * Flag the checking of the parameter constraints is dependent on.
   */
  private Flag flag;

  /**
   * Indicates at which status of the flag the parameter is to be checked.
   */
  private boolean flagConstraint;

  /**
   * List of parameter constraints.
   */
  private List<ParameterConstraint<S>> cons;

  /**
   * Constructs a global parameter constraint specifying that the testing of the
   * parameter given for keeping the parameter constraints given is dependent on
   * the status of the flag given.
   * 
   * @param p parameter possibly to be checked
   * @param c a list of parameter constraints, if the value is null, the parameter is just tested if it is set.
   * @param f flag controlling the checking of the parameter constraints
   * @param flagConstraint indicates at which status of the flag the parameter
   *        is to be checked
   */
  public ParameterFlagGlobalConstraint(Parameter<S, C> p, List<ParameterConstraint<S>> c, Flag f, boolean flagConstraint) {
    param = p;
    flag = f;
    this.flagConstraint = flagConstraint;
    cons = c;
  }

  /**
   * Checks the parameter for its parameter constraints dependent on the status
   * of the given flag. If a parameter constraint is breached a parameter
   * exception is thrown.
   * 
   */
  public void test() throws ParameterException {
    // only check constraints of param if flag is set
    if(flagConstraint == flag.getValue()) {
      if(cons != null) {
        for(ParameterConstraint<? super C> c : cons) {
          c.test(param.getValue());
        }
      }
      else {
        if(!param.isDefined()) {
          throw new UnusedParameterException("Value of parameter " + param.getName() + " is not optional.");
        }
      }
    }
  }

  public String getDescription() {
    StringBuffer description = new StringBuffer();
    if(flagConstraint) {
      description.append("If ").append(flag.getName());
      description.append(" is set, the following constraints for parameter ");
      description.append(param.getName()).append(" have to be fullfilled: ");
      if(cons != null) {
        for(int i = 0; i < cons.size(); i++) {
          ParameterConstraint<? super C> c = cons.get(i);
          if(i > 0) {
            description.append(", ");
          }
          description.append(c.getDescription(param.getName()));
        }
      }
      else {
        description.append(param.getName()+" must be set.");
      }
    }
    return description.toString();
  }
}