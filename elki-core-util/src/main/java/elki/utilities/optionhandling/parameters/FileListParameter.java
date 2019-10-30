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
package elki.utilities.optionhandling.parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.ParameterException;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying a list of files.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class FileListParameter extends ListParameter<FileListParameter, List<Path>> {
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

  @Override
  public String getValueAsString() {
    StringBuilder buf = new StringBuilder(100);
    for(Path p : getValue()) {
      buf.append(p.toString()).append(LIST_SEP);
    }
    buf.setLength(buf.length() > LIST_SEP.length() ? buf.length() - LIST_SEP.length() : buf.length());
    return buf.toString();
  }

  @Override
  public String getDefaultValueAsString() {
    StringBuilder buf = new StringBuilder(100);
    for(Path p : getDefaultValue()) {
      buf.append(p.toString()).append(LIST_SEP);
    }
    buf.setLength(buf.length() > LIST_SEP.length() ? buf.length() - LIST_SEP.length() : buf.length());
    return buf.toString();
  }

  @Override
  protected List<Path> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      List<Path> r = new ArrayList<>(l.size());
      // do extra validation:
      for(Object o : l) {
        if(o instanceof Path) {
          r.add((Path) o);
        }
        else if(o instanceof File) {
          r.add(((File) o).toPath());
        }
        else if(o instanceof String) {
          r.add(Paths.get((String) o));
        }
        else {
          throw new WrongParameterValueException(this, obj.toString(), "expected a List<Path> or a String.");
        }
      }
      return r;
    }
    catch(ClassCastException e) {
      // continue with others
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      ArrayList<Path> fileValue = new ArrayList<>(values.length);
      for(String val : values) {
        fileValue.add(Paths.get(val));
      }
      return fileValue;
    }
    throw new WrongParameterValueException(this, obj.toString(), "expected a String containing file names.");
  }

  @Override
  protected boolean validate(List<Path> obj) throws ParameterException {
    if(!super.validate(obj)) {
      return false;
    }
    if(filesType.equals(FilesType.INPUT_FILES)) {
      for(Path file : obj) {
        try {
          if(!Files.exists(file)) {
            throw new WrongParameterValueException(this, getValueAsString(), "File \"" + file + "\" does not exist.");
          }
        }
        catch(SecurityException e) {
          throw new WrongParameterValueException(this, getValueAsString(), "File \"" + file + "\" cannot be read, access denied.", e);
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

  /**
   * Get the parameter.
   *
   * @param config Parameterization
   * @param consumer Output consumer
   * @return {@code true} if valid
   */
  public boolean grab(Parameterization config, Consumer<List<Path>> consumer) {
    if(config.grab(this)) {
      if(consumer != null) {
        consumer.accept(getValue());
      }
      return true;
    }
    return false;
  }
}
