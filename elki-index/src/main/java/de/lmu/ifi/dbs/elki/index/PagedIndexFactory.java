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
package de.lmu.ifi.dbs.elki.index;

import java.io.Externalizable;

import de.lmu.ifi.dbs.elki.persistent.MemoryPageFileFactory;
import de.lmu.ifi.dbs.elki.persistent.Page;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for tree-based indexes.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory,interface
 * @composed - - - PageFileFactory
 * 
 * @param <O> Object type
 */
public abstract class PagedIndexFactory<O> implements IndexFactory<O> {
  /**
   * Page file factory.
   */
  private PageFileFactory<?> pageFileFactory;

  /**
   * Constructor.
   * 
   * @param pageFileFactory
   *        Page file factory
   */
  public PagedIndexFactory(PageFileFactory<?> pageFileFactory) {
    super();
    this.pageFileFactory = pageFileFactory;
  }

  /**
   * Make the page file for this index.
   * 
   * @param <N>
   *        page type
   * @param cls
   *        Class information
   * @return Page file
   */
  protected <N extends Page & Externalizable> PageFile<N> makePageFile(Class<N> cls) {
    @SuppressWarnings("unchecked")
    final PageFileFactory<N> castFactory = (PageFileFactory<N>) pageFileFactory;
    return castFactory.newPageFile(cls);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Optional parameter that specifies the factory type of pagefile to use
     * for the index.
     */
    public static final OptionID PAGEFILE_ID = new OptionID("index.pagefile", "The pagefile factory for storing the index.");

    /**
     * Page file factory.
     */
    protected PageFileFactory<?> pageFileFactory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PageFileFactory<?>> pffP = new ObjectParameter<>(PAGEFILE_ID, PageFileFactory.class, MemoryPageFileFactory.class);
      if(config.grab(pffP)) {
        pageFileFactory = pffP.instantiateClass(config);
      }
    }

    @Override
    protected abstract PagedIndexFactory<O> makeInstance();
  }
}
