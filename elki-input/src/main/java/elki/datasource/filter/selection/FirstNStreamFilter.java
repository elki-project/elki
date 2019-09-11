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
package elki.datasource.filter.selection;

import elki.datasource.bundle.BundleMeta;
import elki.datasource.filter.AbstractStreamFilter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Keep only the first N elements of the data source.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FirstNStreamFilter extends AbstractStreamFilter {
  /**
   * Remaining entries to keep
   */
  protected int n;

  /**
   * Constructor.
   * 
   * @param n number of entries to keep
   */
  public FirstNStreamFilter(int n) {
    super();
    this.n = n;
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        return ev;
      case NEXT_OBJECT:
        if(n == 0) {
          return Event.END_OF_STREAM;
        }
        --n;
        return ev;
      }
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option ID for the sample size
     */
    public static final OptionID SIZE_ID = new OptionID("first.n", "Number of objects to keep.");

    /**
     * Number of objects to keep
     */
    protected int n;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(SIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> n = x);
    }

    @Override
    public FirstNStreamFilter make() {
      return new FirstNStreamFilter(n);
    }
  }
}
