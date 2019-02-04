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
package de.lmu.ifi.dbs.elki.persistent;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract page file factory.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <P> Page type
 */
public abstract class AbstractPageFileFactory<P extends Page> implements PageFileFactory<P> {
  /**
   * Holds the value of {@link Parameterizer#PAGE_SIZE_ID}.
   */
  protected int pageSize;

  /**
   * Constructor.
   * 
   * @param pageSize Page size
   */
  public AbstractPageFileFactory(int pageSize) {
    super();
    this.pageSize = pageSize;
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Parameterization class.
   * 
   * @hidden
   * 
   * @author Erich Schubert
   * 
   * @param <P> Page type
   */
  public static abstract class Parameterizer<P extends Page> extends AbstractParameterizer {
    /**
     * Parameter to specify the size of a page in bytes, must be an integer
     * greater than 0.
     */
    public static final OptionID PAGE_SIZE_ID = new OptionID("pagefile.pagesize", "The size of a page in bytes.");

    /**
     * Page size
     */
    protected int pageSize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter pageSizeP = new IntParameter(PAGE_SIZE_ID, 4000) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pageSizeP)) {
        pageSize = pageSizeP.getValue();
      }
    }

    @Override
    abstract protected PageFileFactory<P> makeInstance();
  }
}
