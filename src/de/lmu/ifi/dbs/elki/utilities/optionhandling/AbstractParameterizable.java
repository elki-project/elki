package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Abstract superclass for classes parameterizable. Provides the option handler
 * and the parameter array.
 *
 * @author Elke Achtert
 */
public abstract class AbstractParameterizable extends AbstractLoggable
    implements Parameterizable {

    /**
     * OptionHandler for handling options.
     */
    protected OptionHandler optionHandler;

    /**
     * Holds the currently set parameter array.
     */
    private String[] currentParameterArray = new String[0];

    /**
     * Creates a new AbstractParameterizable that provides the option handler
     * and the parameter array.
     */
    public AbstractParameterizable() {
        super(LoggingConfiguration.DEBUG);
        optionHandler = new OptionHandler(new TreeMap<String, Option<?>>(), this.getClass().getName());
    }

    /**
     * Adds the given Option to the set of Options known to this Parameterizable.
     *
     * @param option the Option to add to the set of known Options of this Parameterizable
     */
    protected void addOption(Option<?> option) {
        this.optionHandler.put(option);
    }

    /**
     * Deletes the given Option from the set of Options known to this Parameterizable.
     *
     * @param option the Option to remove from the set of Options known to this Parameterizable
     * @throws UnusedParameterException if the given Option is unknown
     */
    protected void deleteOption(Option<?> option) throws UnusedParameterException {
        this.optionHandler.remove(option.getName());
    }

    /**
     * @see Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = optionHandler.grabOptions(args);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Sets the difference of the first array minus the second array as the
     * currently set parameter array.
     *
     * @param complete the complete array
     * @param part     an array that contains only elements of the first array
     */
    protected final void setParameters(String[] complete, String[] part) {
        currentParameterArray = Util.parameterDifference(complete, part);
    }

    /**
     * @see Parameterizable#getParameters()
     */
    public final String[] getParameters() {
        String[] param = new String[currentParameterArray.length];
        System.arraycopy(currentParameterArray, 0, param, 0,
                         currentParameterArray.length);
        return param;
    }

    /**
     * @see Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings() {
        try {
            List<AttributeSettings> settings = new ArrayList<AttributeSettings>();
            AttributeSettings mySettings = new AttributeSettings(this);
            optionHandler.addOptionSettings(mySettings);
            settings.add(mySettings);
            return settings;
        }
        catch (UnusedParameterException e) {
            throw new RuntimeException("This should never happen! ", e);
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#description()
     */
    public String description() {
        return optionHandler.usage("");
    }

    /**
     * @see Parameterizable#inlineDescription()
     */
    public String inlineDescription() {
        return optionHandler.usage("", false);
    }

    /**
     * Returns an usage-String by calling
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler#usage(String)}.
     *
     * @param message some error-message, if needed (may be null or empty String)
     * @return an usage-String
     */
    protected String description(String message) {
        return this.optionHandler.usage(message);
    }

    /**
     * Returns an usage-String by calling
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler#usage(String,boolean)}.
     *
     * @param message    some error-message, if needed (may be null or empty String)
     * @param standalone whether the class using the OptionHandler provides a main
     *                   method
     * @return an usage-String
     */
    protected String description(String message, boolean standalone) {
        return this.optionHandler.usage(message, standalone);
    }

    /**
     * Returns true if the value of the given option is set, false otherwise.
     *
     * @param option The option should be asked for without leading &quot;-&quot;
     *               or closing &quot;:&quot;.
     * @return boolean true if the value of the given option is set, false
     *         otherwise
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler#isSet(Option)
     */
    protected boolean isSet(Option<?> option) {
        return option.isSet();
    }

    /**
     * Returns the value of the given parameter by calling
     * {@link Parameter#getValue()}.
     *
     * @param parameter the parameter to get the value of
     * @return the value of given parameter
     * @throws IllegalStateException if this Parameterizable object has not been initialized
     *                               properly (e.g. the setParameters(String[]) method has been failed
     *                               to be called).
     * @see Parameter#getValue()
     */
    protected <T> T getParameterValue(Parameter<T, ?> parameter) throws IllegalStateException {
        try {
            return parameter.getValue();
        }
        catch (UnusedParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getPossibleOptions()
     */
    public Option<?>[] getPossibleOptions() {
        return optionHandler.getOptions();
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#checkGlobalParameterConstraints()
     */
    public void checkGlobalParameterConstraints() throws ParameterException {
        this.optionHandler.checkGlobalParameterConstraints();
    }
}
