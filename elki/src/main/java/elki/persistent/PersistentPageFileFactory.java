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
package elki.persistent;

import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Page file factory for disk-based page files.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - PersistentPageFile
 * 
 * @param <P> Page type
 */
public class PersistentPageFileFactory<P extends ExternalizablePage> extends AbstractPageFileFactory<P> {
  /**
   * File name.
   */
  private String fileName;

  /**
   * Constructor.
   * 
   * @param pageSize Page size
   */
  public PersistentPageFileFactory(int pageSize, String fileName) {
    super(pageSize);
    this.fileName = fileName;
  }

  @Override
  public PageFile<P> newPageFile(Class<P> cls) {
    if(fileName == null) {
      throw new AbortException("Disk-backed page file may only be instantiated once!");
    }
    PersistentPageFile<P> pfile = new PersistentPageFile<>(pageSize, fileName, cls);
    fileName = null; // To avoid double instantiation.
    return pfile;
  }

  /**
   * Parameterization class.
   * 
   * @hidden
   * 
   * @author Erich Schubert
   */
  public static class Par extends AbstractPageFileFactory.Par<ExternalizablePage> {
    /**
     * File name.
     */
    private String fileName;

    /**
     * Optional parameter that specifies the name of the file storing the index.
     */
    public static final OptionID FILE_ID = new OptionID("pagefile.file", "The name of the file storing the page file.");

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new FileParameter(FILE_ID, FileParameter.FileType.OUTPUT_FILE) //
          .grab(config, x -> fileName = x.getPath());
    }

    @Override
    public PersistentPageFileFactory<ExternalizablePage> make() {
      return new PersistentPageFileFactory<>(pageSize, fileName);
    }
  }
}
