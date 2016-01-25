package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter constraint specifying that a parameter list 
 * is not allowed to have duplicate values.
 * 
 * @author Arthur Zimek
 * @since 0.5.5
 */
public class ListParameterNoDuplicateValueConstraint<T extends Object> implements ParameterConstraint<List<T>> {

  /**
   * Constructs a Not-Equal-Value parameter constraint. That is, the
   * elements of a list of parameter values are not allowed to have equal
   * values.
   * 
   */
  public ListParameterNoDuplicateValueConstraint() {
  }



  /**
   * Checks if the elements of the list of parameter values do have different
   * values. If not, a parameter exception is thrown.
   * 
   */
  @Override
  public void test(List<T> list) throws ParameterException {
    Set<T> values = new HashSet<>();

    for(T pv : list) {
      if(!values.add(pv)) {
        Object[] parametervaluesarr = list.toArray();
        throw new WrongParameterValueException("Global Parameter Constraint Error:\n" + "Parameter values must have different values. Current values: " + Arrays.deepToString(parametervaluesarr) + ".\n");
      }
    }
  }
  
  @Override
  public String getDescription(String parameterName) {
    return "Values for parameter "+parameterName+" must have different values.";
  }




}