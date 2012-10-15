package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Applies constraints to all elements of a list.
 * 
 * @author Erich Schubert
 */
public class ListEachConstraint<T> implements ParameterConstraint<List<T>> {
  /**
   * Constraints
   */
  private List<ParameterConstraint<? super T>> constraints;

  /**
   * Constructor.
   */
  public ListEachConstraint() {
    super();
    this.constraints = new ArrayList<ParameterConstraint<? super T>>();
  }

  /**
   * Constructor.
   * 
   * @param constraint Constraint to apply to all elements
   */
  public ListEachConstraint(ParameterConstraint<? super T> constraint) {
    super();
    this.constraints = new ArrayList<ParameterConstraint<? super T>>(1);
    this.constraints.add(constraint);
  }

  /**
   * Add a constraint to this operator.
   * 
   * @param constraint Constraint
   */
  public void addConstraint(ParameterConstraint<? super T> constraint) {
    this.constraints.add(constraint);
  }

  @Override
  public void test(List<T> t) throws ParameterException {
    for (T e : t) {
      for (ParameterConstraint<? super T> c : constraints) {
        c.test(e);
      }
    }
  }

  @Override
  public String getDescription(String parameterName) {
    final String all = "all elements of " + parameterName;
    StringBuilder b = new StringBuilder();
    for (ParameterConstraint<? super T> c : constraints) {
      b.append(c.getDescription(all));
    }
    return b.toString();
  }
}
