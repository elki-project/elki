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
 */
// TODO: turn FileType into a Constraint?
public class FileParameter extends Parameter<File, File> {
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

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    try {
      return getValue().getCanonicalPath();
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected File parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter \"" + getName() + "\": No filename given!");
    }
    if(obj instanceof File) {
      return (File) obj;
    }
    if(obj instanceof String) {
      return new File((String) obj);
    }
    throw new UnspecifiedParameterException("Parameter \"" + getName() + "\": Unsupported value given!");
  }

  /** {@inheritDoc} */
  @Override
  protected boolean validate(File obj) throws ParameterException {
    if(!super.validate(obj)) {
      return false;
    }
    if(fileType.equals(FileType.INPUT_FILE)) {
      try {
        if(!obj.exists()) {
          throw new WrongParameterValueException("Given file " + obj.getPath() + " for parameter \"" + getName() + "\" does not exist!\n");
        }
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
}
