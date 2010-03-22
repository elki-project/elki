package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.List;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a double value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * 
 * @param <D> Distance type 
 */
public class DistanceParameter<D extends Distance<D>> extends Parameter<D, D> {
  /**
   * Distance type
   */
  DistanceFunction<?, D> dist;
  
  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance function
   * @param cons a list of parameter constraints for this double parameter
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, List<ParameterConstraint<D>> cons, D defaultValue) {
    super(optionID, cons, defaultValue);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and optional flag.
   * 
   * @param optionID the unique optionID
   * @param dist distance function
   * @param cons a list of parameter constraints for this double parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, List<ParameterConstraint<D>> cons, boolean optional) {
    this(optionID, dist, cons);
    setOptional(optional);
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraints.
   * 
   * @param optionID the unique optionID
   * @param dist distance function
   * @param constraints a list of parameter constraints for this double
   *        parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, List<ParameterConstraint<D>> constraints) {
    super(optionID, constraints);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance function
   * @param constraint the constraint of this parameter
   * @param defaultValue the default value for this parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, ParameterConstraint<D> constraint, D defaultValue) {
    super(optionID, constraint, defaultValue);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance function
   * @param constraint the constraint of this parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, ParameterConstraint<D> constraint, boolean optional) {
    super(optionID, constraint, optional);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance function
   * @param constraint the constraint of this parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, ParameterConstraint<D> constraint) {
    super(optionID, constraint);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance function
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, D defaultValue) {
    super(optionID, defaultValue);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance function
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, boolean optional) {
    super(optionID, optional);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance function
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist) {
    super(optionID);
    this.dist = dist;
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected D parseValue(Object obj) throws WrongParameterValueException {
    if (dist == null) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a distance value, but the distance function was not set!");
    }
    if (obj == null) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a distance value, but a null value was given!");
    }
    if(dist.nullDistance().getClass().isAssignableFrom(obj.getClass())) {
      return (D) dist.nullDistance().getClass().cast(obj);
    }
    try {
      return dist.valueOf(obj.toString());
    }
    catch(IllegalArgumentException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a distance value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;distance&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<distance>";
  }
}