package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.io.File;
import java.util.Vector;

/**
 * Parameter class for a parameter specifying a list of files.
 *
 * @author Steffi Wanka
 */
public class FileListParameter extends ListParameter<File> {
    /**
     * Available types of the files:
     * {@link #INPUT_FILES} denotes input files,
     * {@link #OUTPUT_FILES} denotes output files.
     */
    public enum FilesType {
        INPUT_FILES,
        OUTPUT_FILES
    }

    /**
     * Specifies the type of the files, i.e. if the files are input or output files.
     */
    private FilesType filesType;

    /**
     * Constructs a file list parameter with the given optionID, and file type.
     *
     * @param optionID  the unique id of this file list parameter
     * @param filesType the file type of this file list parameter
     */
    public FileListParameter(OptionID optionID, FilesType filesType) {
        super(optionID);
        this.filesType = filesType;
    }

    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            String[] files = SPLIT.split(value);
            Vector<File> fileValue = new Vector<File>();
            for (String f : files) {
                fileValue.add(new File(f));
            }
            this.value = fileValue;
        }
    }

    public boolean isValid(String value) throws ParameterException {
        String[] files = SPLIT.split(value);
        if (files.length == 0) {
            throw new WrongParameterValueException("Given list of files for paramter \"" + getName()
                + "\" is either empty or has the wrong format!\nParameter value required:\n" + getDescription());
        }

        if (filesType.equals(FilesType.INPUT_FILES)) {
            for (String f : files) {
                File file = new File(f);
                try {
                    if (!file.exists()) {

                        throw new WrongParameterValueException("Given file " + file.getPath() + " for parameter \"" + getName()
                            + "\" does not exist!\n");
                    }
                }

                catch (SecurityException e) {
                    throw new WrongParameterValueException("Given file \"" + file.getPath() + "\" cannot be read, access denied!\n"
                        + e.getMessage());
                }
            }
        }
        return true;
    }

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;file_1,...,file_n&gt;&quot;
     */
    protected String getParameterType() {
        return "<file_1,...,file_n>";
    }
}
