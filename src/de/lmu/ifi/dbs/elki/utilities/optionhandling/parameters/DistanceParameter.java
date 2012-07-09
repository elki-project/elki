package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
public class DistanceParameter<D extends Distance<D>> extends AbstractParameter<D, D> {
  /**
   * Distance type
   */
  D dist;
  
  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param cons a list of parameter constraints for this double parameter
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, D dist, List<ParameterConstraint<D>> cons, D defaultValue) {
    super(optionID, cons, defaultValue);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param cons a list of parameter constraints for this double parameter
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, List<ParameterConstraint<D>> cons, D defaultValue) {
    super(optionID, cons, defaultValue);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and optional flag.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param cons a list of parameter constraints for this double parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, D dist, List<ParameterConstraint<D>> cons, boolean optional) {
    this(optionID, dist, cons);
    setOptional(optional);
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and optional flag.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
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
   * @param dist distance factory
   * @param constraints a list of parameter constraints for this double
   *        parameter
   */
  public DistanceParameter(OptionID optionID, D dist, List<ParameterConstraint<D>> constraints) {
    super(optionID, constraints);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraints.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param constraints a list of parameter constraints for this double
   *        parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, List<ParameterConstraint<D>> constraints) {
    super(optionID, constraints);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param constraint the constraint of this parameter
   * @param defaultValue the default value for this parameter
   */
  public DistanceParameter(OptionID optionID, D dist, ParameterConstraint<D> constraint, D defaultValue) {
    super(optionID, constraint, defaultValue);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param constraint the constraint of this parameter
   * @param defaultValue the default value for this parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, ParameterConstraint<D> constraint, D defaultValue) {
    super(optionID, constraint, defaultValue);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param constraint the constraint of this parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, D dist, ParameterConstraint<D> constraint, boolean optional) {
    super(optionID, constraint, optional);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param constraint the constraint of this parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, ParameterConstraint<D> constraint, boolean optional) {
    super(optionID, constraint, optional);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param constraint the constraint of this parameter
   */
  public DistanceParameter(OptionID optionID, D dist, ParameterConstraint<D> constraint) {
    super(optionID, constraint);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param constraint the constraint of this parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, ParameterConstraint<D> constraint) {
    super(optionID, constraint);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, D dist, D defaultValue) {
    super(optionID, defaultValue);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, D defaultValue) {
    super(optionID, defaultValue);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, D dist, boolean optional) {
    super(optionID, optional);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist, boolean optional) {
    super(optionID, optional);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   */
  public DistanceParameter(OptionID optionID, D dist) {
    super(optionID);
    this.dist = dist;
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?, D> dist) {
    super(optionID);
    this.dist = (dist != null) ? dist.getDistanceFactory() : null;
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected D parseValue(Object obj) throws WrongParameterValueException {
    if (dist == null) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a distance value, but the distance was not set!");
    }
    if (obj == null) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a distance value, but a null value was given!");
    }
    if(dist.nullDistance().getClass().isAssignableFrom(obj.getClass())) {
      return (D) dist.nullDistance().getClass().cast(obj);
    }
    try {
      return dist.parseString(obj.toString());
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