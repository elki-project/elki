package de.lmu.ifi.dbs.utilities.optionhandling;

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
     * Constructs a file list parameter with the given name, description, and file type
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param filesType   the file type of this file list parameter
     * @deprecated
     */
    @Deprecated
    public FileListParameter(String name, String description, FilesType filesType) {
        super(name, description);
        this.filesType = filesType;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(String)
     */
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

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
     */
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
     * @see Parameter#getParameterType()
     */
    protected String getParameterType() {
        return "<file_1,...,file_n>";
    }
}
