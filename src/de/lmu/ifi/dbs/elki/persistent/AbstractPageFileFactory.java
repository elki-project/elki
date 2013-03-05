package de.lmu.ifi.dbs.elki.persistent;

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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Page file factory for memory page files.
 * 
 * @author Erich Schubert
 * 
 * @param <P> Page type
 */
public abstract class AbstractPageFileFactory<P extends Page> implements PageFileFactory<P> {
  /**
   * Holds the value of {@link Parameterizer#PAGE_SIZE_ID}.
   */
  protected int pageSize;

  /**
   * Holds the value of {@link Parameterizer#CACHE_SIZE_ID}.
   */
  protected long cacheSize;

  /**
   * Constructor.
   * 
   * @param pageSize Page size
   * @param cacheSize Cache size
   */
  public AbstractPageFileFactory(int pageSize, long cacheSize) {
    super();
    this.pageSize = pageSize;
    this.cacheSize = cacheSize;
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Parameterization class.
   * 
   * @apiviz.exclude
   * 
   * @author Erich Schubert
   * 
   * @param <P> Page type
   */
  public static abstract class Parameterizer<P extends Page> extends AbstractParameterizer {
    /**
     * Parameter to specify the size of a page in bytes, must be an integer
     * greater than 0.
     * <p>
     * Default value: {@code 4000}
     * </p>
     * <p>
     * Key: {@code -pagefile.pagesize}
     * </p>
     */
    public static final OptionID PAGE_SIZE_ID = new OptionID("pagefile.pagesize", "The size of a page in bytes.");

    /**
     * Parameter to specify the size of the cache in bytes, must be an integer
     * equal to or greater than 0.
     * <p>
     * Default value: {@link Integer#MAX_VALUE}
     * </p>
     * <p>
     * Key: {@code -pagefile.cachesize}
     * </p>
     */
    public static final OptionID CACHE_SIZE_ID = new OptionID("pagefile.cachesize", "The size of the cache in bytes.");

    /**
     * Page size
     */
    protected int pageSize;

    /**
     * Cache size
     */
    protected long cacheSize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter pageSizeP = new IntParameter(PAGE_SIZE_ID, 4000);
      pageSizeP.addConstraint(new GreaterConstraint(0));
      if (config.grab(pageSizeP)) {
        pageSize = pageSizeP.getValue();
      }

      LongParameter cacheSizeP = new LongParameter(CACHE_SIZE_ID, Long.MAX_VALUE);
      cacheSizeP.addConstraint(new GreaterEqualConstraint(0));
      if (config.grab(cacheSizeP)) {
        cacheSize = cacheSizeP.getValue();
      }
    }

    @Override
    abstract protected PageFileFactory<P> makeInstance();
  }
}
