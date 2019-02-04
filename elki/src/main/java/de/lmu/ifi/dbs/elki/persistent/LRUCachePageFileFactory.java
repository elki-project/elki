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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Page file factory for memory page files.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - LRUCache
 * @composed - - - PageFileFactory
 * 
 * @param <P> Page type
 */
public class LRUCachePageFileFactory<P extends Page> implements PageFileFactory<P> {
  /**
   * Inner page file factory.
   */
  private PageFileFactory<P> pageFileFactory;

  /**
   * Cache size, in bytes.
   */
  private int cacheSize;

  /**
   * Constructor.
   * 
   * @param pageFileFactory Inner page file
   * @param cacheSize Size of cache, in bytes.
   */
  public LRUCachePageFileFactory(PageFileFactory<P> pageFileFactory, int cacheSize) {
    super();
    this.cacheSize = cacheSize;
    this.pageFileFactory = pageFileFactory;
  }

  @Override
  public PageFile<P> newPageFile(Class<P> cls) {
    PageFile<P> inner = pageFileFactory.newPageFile(cls);
    return new LRUCache<>(cacheSize, inner);
  }

  @Override
  public int getPageSize() {
    return pageFileFactory.getPageSize();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the size of the cache in bytes, must be an integer
     * equal to or greater than 0.
     */
    public static final OptionID CACHE_SIZE_ID = new OptionID("pagefile.cachesize", "The size of the cache in bytes.");

    /**
     * Parameter to specify the inner pagefile.
     */
    public static final OptionID PAGEFILE_ID = new OptionID("pagefile.pagefile", "The backing pagefile for the cache.");

    /**
     * Inner page file factory.
     */
    PageFileFactory<Page> pageFileFactory;

    /**
     * Cache size, in bytes.
     */
    protected int cacheSize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PageFileFactory<Page>> pffP = new ObjectParameter<>(PAGEFILE_ID, PageFileFactory.class, PersistentPageFileFactory.class);
      if(config.grab(pffP)) {
        pageFileFactory = pffP.instantiateClass(config);
      }

      IntParameter cacheSizeP = new IntParameter(CACHE_SIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(cacheSizeP)) {
        cacheSize = cacheSizeP.getValue();
      }
    }

    @Override
    protected LRUCachePageFileFactory<Page> makeInstance() {
      return new LRUCachePageFileFactory<>(pageFileFactory, cacheSize);
    }
  }
}
