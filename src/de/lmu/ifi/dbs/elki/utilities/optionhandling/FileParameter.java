package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.io.File;

/**
 * Parameter class for a parameter specifying a file.
 *
 * @author Steffi Wanka
 */
public class FileParameter extends Parameter<File, Object> {
    /**
     * Available file types:
     * {@link #INPUT_FILE} denotes an input file,
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
     * The file type of this file parameter. Specifies if the file is an input of output file.
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
     * Constructs a file parameter with the given optionID, file type,
     * and optional flag.
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
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            this.value = new File(value);
        }
    }

    @Override
    public boolean isValid(String value) throws ParameterException {
        if (value == null) {
            throw new WrongParameterValueException("Parameter \"" + getName()
                + "\": No filename given!\nParameter description: " + getDescription());
        }

        if (fileType.equals(FileType.INPUT_FILE)) {
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

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;file_&gt;&quot;
     */
    @Override
    protected String getParameterType() {
        return "<file>";
    }
}
