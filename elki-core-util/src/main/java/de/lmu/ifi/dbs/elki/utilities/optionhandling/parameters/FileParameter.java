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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a file.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
// TODO: turn FileType into a Constraint?
public class FileParameter extends AbstractParameter<FileParameter, File> {
  /**
   * Available file types: {@link #INPUT_FILE} denotes an input file,
   * {@link #OUTPUT_FILE} denotes an output file.
   */
  public enum FileType {
    /**
     * Input files (i.e. read only)
     */
    INPUT_FILE,
    /**
     * Output files
     */
    OUTPUT_FILE
  }

  /**
   * The file type of this file parameter. Specifies if the file is an input of
   * output file.
   */
  private FileType fileType;

  /**
   * Constructs a file parameter with the given optionID, and file type.
   * 
   * @param optionID optionID the unique id of the option
   * @param fileType the file type of this file parameter
   */
  public FileParameter(OptionID optionID, FileType fileType) {
    super(optionID);
    this.fileType = fileType;
  }

  /**
   * Constructs a file parameter with the given optionID, file type, and
   * optional flag.
   * 
   * @param optionID optionID the unique id of the option
   * @param fileType the file type of this file parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public FileParameter(OptionID optionID, FileType fileType, boolean optional) {
    this(optionID, fileType);
    setOptional(optional);
  }

  @Override
  public String getValueAsString() {
    try {
      return getValue().getCanonicalPath();
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected File parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(obj instanceof File) {
      return (File) obj;
    }
    if(obj instanceof String) {
      return new File((String) obj);
    }
    throw new WrongParameterValueException("Parameter \"" + getOptionID().getName() + "\": Unsupported value given!");
  }

  @Override
  protected boolean validate(File obj) throws ParameterException {
    if(!super.validate(obj)) {
      return false;
    }
    if(fileType.equals(FileType.INPUT_FILE)) {
      try {
        if(obj.exists()) {
          return true;
        }
        throw new WrongParameterValueException("Given file " + obj.getPath() + " for parameter \"" + getOptionID().getName() + "\" does not exist!\n");
      }
      catch(SecurityException e) {
        throw new WrongParameterValueException("Given file \"" + obj.getPath() + "\" cannot be read, access denied!\n" + e.getMessage());
      }
    }
    return true;
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;file_&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<file>";
  }

  /**
   * Get the file type (input / output)
   * 
   * @return file type
   */
  public FileType getFileType() {
    return fileType;
  }
}
