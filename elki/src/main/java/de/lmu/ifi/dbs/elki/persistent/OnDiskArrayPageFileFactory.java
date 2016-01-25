package de.lmu.ifi.dbs.elki.persistent;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Page file factory for disk-based page files.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has OnDiskArrayPageFile
 * 
 * @param <P> Page type
 */
public class OnDiskArrayPageFileFactory<P extends ExternalizablePage> extends AbstractPageFileFactory<P> {
  /**
   * File name.
   */
  private String fileName;

  /**
   * Constructor.
   * 
   * @param pageSize Page size
   */
  public OnDiskArrayPageFileFactory(int pageSize, String fileName) {
    super(pageSize);
    this.fileName = fileName;
  }

  @Override
  public PageFile<P> newPageFile(Class<P> cls) {
    if (fileName == null) {
      throw new AbortException("Disk-backed page file may only be instantiated once!");
    }
    OnDiskArrayPageFile<P> pfile = new OnDiskArrayPageFile<>(pageSize, fileName);
    fileName = null; // To avoid double instantiation.
    return pfile;
  }

  /**
   * Parameterization class.
   * 
   * @apiviz.exclude
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractPageFileFactory.Parameterizer<ExternalizablePage> {
    /**
     * File name.
     */
    private String fileName;

    /**
     * Optional parameter that specifies the name of the file storing the index.
     * <p>
     * Key: {@code -pagefile.file}
     * </p>
     */
    public static final OptionID FILE_ID = new OptionID("pagefile.file", "The name of the file storing the page file.");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter fileNameP = new FileParameter(FILE_ID, FileParameter.FileType.OUTPUT_FILE);
      if (config.grab(fileNameP)) {
        fileName = fileNameP.getValue().getPath();
      }
    }

    @Override
    protected OnDiskArrayPageFileFactory<ExternalizablePage> makeInstance() {
      return new OnDiskArrayPageFileFactory<>(pageSize, fileName);
    }
  }
}
