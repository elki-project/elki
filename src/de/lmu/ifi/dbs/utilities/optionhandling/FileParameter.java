package de.lmu.ifi.dbs.utilities.optionhandling;

import java.io.File;

/**
 * Parameter class for a parameter specifying a file.
 *
 * @author Steffi Wanka
 */
public class FileParameter extends Parameter<File, Object> {

  /**
   * Constant indicating an input file
   */
  public static final int FILE_IN = 1;

  /**
   * Constant indication an output file
   */
  public static final int FILE_OUT = 2;

  /**
   * The file type of this file parameter. Specifies if the file is an input of output file.
   */
  private int fileType;

  /**
   * Constructs a file parameter with the given name, description, and file type.
   *
   * @param name        the parameter name
   * @param description the parameter description
   * @param fileType    the fily type of this file parameter
   */
  public FileParameter(String name, String description, int fileType) {
    super(name, description);
    this.fileType = fileType;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
   */
  public void setValue(String value) throws ParameterException {
    if (isValid(value)) {
      this.value = new File(value);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
   */
  public boolean isValid(String value) throws ParameterException {
    if (value == null) {
      throw new WrongParameterValueException("Parameter \"" + getName()
                                             + "\": No filename given!\nParameter description: " + getDescription());
    }

    if (fileType == FILE_IN) {
      File file = new File(value);
      try {
        if (!file.exists()) {
          throw new WrongParameterValueException("Given file " + file.getPath()
                                                 + " for parameter \"" + getName() + "\" does not exist!\n");
        }
      }
      catch (SecurityException e) {
        throw new WrongParameterValueException("Given file \"" + file.getPath()
                                               + "\" cannot be read, access denied!\n" + e.getMessage());
      }
    }
    return true;
	}
}
