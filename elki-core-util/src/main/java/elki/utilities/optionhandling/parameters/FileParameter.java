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
package elki.utilities.optionhandling.parameters;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.ParameterException;
import elki.utilities.optionhandling.UnspecifiedParameterException;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying a file.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
// TODO: turn FileType into a Constraint?
public class FileParameter extends AbstractParameter<FileParameter, URI> {
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

  @Override
  public String getValueAsString() {
    final URI v = getValue();
    return v == null ? null : "file".equals(v.getScheme()) ? Paths.get(v).toString() : v.normalize().toString();
  }

  @Override
  protected URI parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(obj instanceof URI) {
      return (URI) obj;
    }
    if(obj instanceof URL) {
      try {
        return ((URL) obj).toURI();
      }
      catch(URISyntaxException e) {
        throw new WrongParameterValueException(this, obj.toString(), e.getMessage());
      }
    }
    if(obj instanceof Path) {
      return ((Path) obj).toUri();
    }
    if(obj instanceof File) {
      return ((File) obj).toURI();
    }
    if(obj instanceof String) {
      String str = (String) obj;
      if(!str.isEmpty() && str.charAt(0) != '/') {
        try {
          URI u = new URI(str);
          if(u.getScheme() != null) {
            return u;
          }
        }
        catch(URISyntaxException e) {
          // Fallback to path-based parsing below.
        }
      }
      return Paths.get(str).toUri();
    }
    throw new WrongParameterValueException(this, obj.toString(), "Unsupported value");
  }

  @Override
  protected boolean validate(URI obj) throws ParameterException {
    if(!super.validate(obj)) {
      return false;
    }
    if(fileType.equals(FileType.INPUT_FILE)) {
      try {
        if(FileUtil.exists(obj)) {
          return true;
        }
        throw new WrongParameterValueException("Given file " + obj + " for parameter \"" + getOptionID().getName() + "\" does not exist!");
      }
      catch(SecurityException e) {
        throw new WrongParameterValueException("Given file \"" + obj + "\" cannot be read, access denied!\n" + e.getMessage());
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

  /**
   * Get the parameter.
   *
   * @param config Parameterization
   * @param consumer Output consumer
   * @return {@code true} if valid
   */
  public boolean grab(Parameterization config, Consumer<URI> consumer) {
    if(config.grab(this)) {
      if(consumer != null) {
        consumer.accept(getValue());
      }
      return true;
    }
    return false;
  }
}
