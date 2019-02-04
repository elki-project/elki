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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a list of files.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class FileListParameter extends ListParameter<FileListParameter, List<File>> {
  /**
   * Available types of the files: {@link #INPUT_FILES} denotes input files,
   * {@link #OUTPUT_FILES} denotes output files.
   */
  public enum FilesType {
    /**
     * Input files (i.e. read only)
     */
    INPUT_FILES,
    /**
     * Output files
     */
    OUTPUT_FILES
  }

  /**
   * Specifies the type of the files, i.e. if the files are input or output
   * files.
   */
  private FilesType filesType;

  /**
   * Constructs a file list parameter with the given optionID, and file type.
   * 
   * @param optionID the unique id of this file list parameter
   * @param filesType the file type of this file list parameter
   */
  public FileListParameter(OptionID optionID, FilesType filesType) {
    super(optionID);
    this.filesType = filesType;
  }

  // TODO: Add remaining constructors.

  @Override
  public String getValueAsString() {
    StringBuilder buf = new StringBuilder();
    List<File> val = getValue();
    Iterator<File> veciter = val.iterator();
    while(veciter.hasNext()) {
      buf.append(veciter.next());
      if(veciter.hasNext()) {
        buf.append(LIST_SEP);
      }
    }
    return buf.toString();
  }

  @Override
  public String getDefaultValueAsString() {
    StringBuilder buf = new StringBuilder();
    List<File> val = getDefaultValue();
    Iterator<File> veciter = val.iterator();
    while(veciter.hasNext()) {
      buf.append(veciter.next());
      if(veciter.hasNext()) {
        buf.append(LIST_SEP);
      }
    }
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<File> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for(Object o : l) {
        if(!(o instanceof File)) {
          throw new WrongParameterValueException(this, obj.toString(), "expected a List<File> or a String.");
        }
      }
      // TODO: can we use reflection to get extra checks?
      return (List<File>) l;
    }
    catch(ClassCastException e) {
      // continue with others
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      ArrayList<File> fileValue = new ArrayList<>(values.length);
      for(String val : values) {
        fileValue.add(new File(val));
      }
      return fileValue;
    }
    throw new WrongParameterValueException(this, obj.toString(), "expected a String containing file names.");
  }

  @Override
  protected boolean validate(List<File> obj) throws ParameterException {
    if(!super.validate(obj)) {
      return false;
    }
    if(filesType.equals(FilesType.INPUT_FILES)) {
      for(File file : obj) {
        try {
          if(!file.exists()) {
            throw new WrongParameterValueException(this, getValueAsString(), "File \"" + file.getPath() + "\" does not exist.");
          }
        }

        catch(SecurityException e) {
          throw new WrongParameterValueException(this, getValueAsString(), "File \"" + file.getPath() + "\" cannot be read, access denied.", e);
        }
      }
    }
    return true;
  }

  @Override
  public int size() {
    return getValue().size();
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;file_1,...,file_n&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<file_1,...,file_n>";
  }
}
