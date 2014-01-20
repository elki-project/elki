package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Parameter class for a parameter specifying a double value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
@Deprecated
public class DistanceParameter extends DoubleParameter {
  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, Double dist, Double defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param dist distance factory
   * @param defaultValue the default value for this double parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?> dist, Double defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, Double dist, boolean optional) {
    super(optionID);
    setOptional(true);
  }

  /**
   * Constructs a double parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?> dist, boolean optional) {
    super(optionID);
    setOptional(true);
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param dist distance factory
   */
  public DistanceParameter(OptionID optionID, DistanceFunction<?> dist) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
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