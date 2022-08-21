/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.persistent;

import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new ObjectParameter<PageFileFactory<Page>>(PAGEFILE_ID, PageFileFactory.class, PersistentPageFileFactory.class) //
          .grab(config, x -> pageFileFactory = x);
      new IntParameter(CACHE_SIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> cacheSize = x);
    }

    @Override
    public LRUCachePageFileFactory<Page> make() {
      return new LRUCachePageFileFactory<>(pageFileFactory, cacheSize);
    }
  }
}
