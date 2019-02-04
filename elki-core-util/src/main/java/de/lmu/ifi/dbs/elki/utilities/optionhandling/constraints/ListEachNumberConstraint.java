/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Applies numeric constraints to all elements of a list.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @composed - - * AbstractNumberConstraint
 */
public class ListEachNumberConstraint<T> implements ParameterConstraint<T> {
  /**
   * Constraints
   */
  private List<AbstractNumberConstraint> constraints;

  /**
   * Constructor.
   */
  public ListEachNumberConstraint() {
    super();
    this.constraints = new ArrayList<>();
  }

  /**
   * Constructor.
   *
   * @param constraint Constraint to apply to all elements
   */
  public ListEachNumberConstraint(AbstractNumberConstraint constraint) {
    super();
    this.constraints = new ArrayList<>(1);
    this.constraints.add(constraint);
  }

  /**
   * Add a constraint to this operator.
   *
   * @param constraint Constraint
   */
  public void addConstraint(AbstractNumberConstraint constraint) {
    this.constraints.add(constraint);
  }

  @Override
  public void test(T t) throws ParameterException {
    if(t instanceof int[]) {
      for(int e : ((int[]) t)) {
        Number n = e; // Auto-boxing. :-(
        for(AbstractNumberConstraint c : constraints) {
          c.test(n);
        }
      }
    }
    else if(t instanceof double[]) {
      for(double e : ((double[]) t)) {
        Number n = e; // Auto-boxing. :-(
        for(AbstractNumberConstraint c : constraints) {
          c.test(n);
        }
      }
    }
    else {
      throw new IllegalArgumentException("ListEachConstraint currently can only be used with int[] and double[]. Please contribute patches.");
    }
  }

  @Override
  public String getDescription(String parameterName) {
    final String all = "all elements of " + parameterName;
    StringBuilder b = new StringBuilder(1000);
    for(AbstractNumberConstraint c : constraints) {
      b.append(c.getDescription(all));
    }
    return b.toString();
  }
}
