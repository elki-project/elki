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

/**
 * Page file factory for memory page files.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - MemoryPageFile
 * 
 * @param <P> Page type
 */
public class MemoryPageFileFactory<P extends Page> extends AbstractPageFileFactory<P> {
  /**
   * Constructor.
   * 
   * @param pageSize Size of a page
   */
  public MemoryPageFileFactory(int pageSize) {
    super(pageSize);
  }

  @Override
  public PageFile<P> newPageFile(Class<P> cls) {
    return new MemoryPageFile<>(pageSize);
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractPageFileFactory.Parameterizer<Page> {
    @Override
    protected MemoryPageFileFactory<Page> makeInstance() {
      return new MemoryPageFileFactory<>(pageSize);
    }    
  }
}
