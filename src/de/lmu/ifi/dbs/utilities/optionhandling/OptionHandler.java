package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import sun.net.dns.ResolverConfiguration.Options;
import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.output.PrettyPrinter;

/**
 * Provides an OptionHandler for holding the given options.
 * <p/>
 * The options specified are stored in a &lt;String,Option&gt;-Map ({@link #java.util.Map}) with the 
 * names of the options being the keys. New options can be added by using one of the put-methods 
 * ({@link #put(Map)}, {@link #put(Option)}, {@link #put(String, Option)}). <br>
 * 
 * <br>
 * <b>Example for usage</b><br>
 * <p/> <p/>
 * 
 * <pre>
 *                 public static void main(String[] args)
 *                  {
 *                      final String FILE = &quot;f&quot;;
 *                      final String MINSUPPORT = &quot;ms&quot;;
 *                      final String MINCONFIDENCE = &quot;mc&quot;;
 *                      final String NUMBER_OF_ITEMS = &quot;i&quot;;
 *                      final String DBSCAN_EPSILON = &quot;eps&quot;;
 *                      final String DBSCAN_MINPTS = &quot;minPts&quot;;
 *                      final String VERBOSE = &quot;v&quot;;
 *                 &lt;p/&gt;
 *                      TreeMap options = new TreeMap();
 *                      options.put(FILE, new Parameter(FILE, &quot;&lt;inputfile&gt; datafile&quot;));
 *                      options.put(MINSUPPORT, new Parameter(MINSUPPORT,&quot;&lt;minsupport&gt; percent&quot;));
 *                      options.put(MINCONFIDENCE, new Parameter(MINCONFIDENCE, &quot;&lt;minConfidence&gt; percent&quot;));
 *                      options.put(NUMBER_OF_ITEMS, new Parameter(NUMBER_OF_ITEMS,&quot;&lt;numberOfItems&gt; number of items in the datafile&quot;));
 *                      options.put(DBSCAN_EPSILON, new Parameter(DBSCAN_EPSILON,&quot;&lt;epsilon&gt; epsilon for ModeDBSCAN\n(should be very small, recommended is at most 0.2).&quot;));
 *                      options.put(DBSCAN_MINPTS, new Parameter(&quot;&lt;minPts&gt; minPts for ModeDBSCAN&quot;));
 *                      options.put(VERBOSE, new Flag(VERBOSE,&quot;flag causes full output&quot;));
 *                      OptionHandler optionHandler = new OptionHandler(options, &quot;java myPackage.myProgram&quot;);
 *                      try
 *                      {
 *                          optionHandler.grabOptions(args);
 *                      }
 *                      catch(NoParameterValueException npve)
 *                      {
 *                          System.err.println(optionHandler.usage(npve.getMessage()));
 *                          System.exit(1);
 *                      }                  
 *                  }
 * </pre>
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 * @version 1.0 gamma (2005-07-28)
 */
public class OptionHandler extends AbstractLoggable {
	/**
	 * The newline-String dependent on the system.
	 */
	public final static String NEWLINE = System.getProperty("line.separator");

	/**
	 * Prefix of optionmarkers on the commandline. <p/> The optionmarkers are
	 * supposed to be given on the commandline with leading -.
	 */
	public static final String OPTION_PREFIX = "-";

	/**
	 * The programCall as it should be denoted in an eventual usage-message.
	 */
	private String programCall;

	/**
	 * Holds the parameter array as given to the last call of
	 * {@link #grabOptions(String[]) grabOptions(String[])}.
	 */
	private String[] currentParameters = new String[0];

	
	/**
	 * Contains the optionHandler's options with the option names being the keys.
	 */
	private Map<String, Option> parameters;
	
	/**
	 * Contains constraints addressing several parameters
	 */
	private List<GlobalParameterConstraint> globalParameterConstraints;
	
	
	/**
	 * Provides an OptionHandler.
	 * <p/> The options are specified in the given TreeMap with the option names
	 * being as keys. 
	 * Leading &quot;-&quot; do not have to be specified since
	 * OptionHandler will provide them. 
	 * 
	 * @param parameters Map containing the options 
	 * @param programCall String for the program-call using this OptionHandler (for
	 *            usage in usage(String))
	 */
	public OptionHandler(Map<String, Option> parameters, String programCall) {
		super(LoggingConfiguration.DEBUG);
		this.parameters = parameters;
		this.programCall = programCall;
		this.globalParameterConstraints = new ArrayList<GlobalParameterConstraint>();
	}

	/**
	 * Reads the options out of a given String-array (usually the args of any
	 * main-method).
	 * 
	 * @param currentOptions
	 *            an array of given options, flags without values. E.g. the args
	 *            of some main-method. In this array every option should have a
	 *            leading &quot;-&quot;.
	 * @return String[] an array containing the unexpected parameters in the
	 *         given order. Parameters are treated as unexpected if they are not
	 *         known to the optionhandler or if they were already read.
	 * @throws NoParameterValueException
	 *             if a parameter, for which a value is required, has none (e.g.
	 *             because the next value is itself some option)
	 */
	public String[] grabOptions(String[] currentOptions)
			throws NoParameterValueException,ParameterException {
		List<String> unexpectedParameters = new ArrayList<String>();
		List<String> parameterArray = new ArrayList<String>();

		Vector<String> possibleParameters = new Vector<String>();
		Vector<String> possibleFlags = new Vector<String>();

		for (Map.Entry<String, Option> option : parameters.entrySet()) {

			if (option.getValue() instanceof Parameter) {
				possibleParameters.add(new String(OPTION_PREFIX
						+ option.getKey()));
			}

			else if (option.getValue() instanceof Flag) {
				possibleFlags.add(new String(OPTION_PREFIX + option.getKey()));
			}

			else {
				// TODO
			}
		}

		for (int i = 0; i < currentOptions.length; i++) {
			if (!currentOptions[i].startsWith(OPTION_PREFIX)) {
				throw new NoParameterValueException(currentOptions[i]
						+ " is no parameter!");
			}

			String noPrefixOption = currentOptions[i].substring(1);
			if (possibleParameters.contains(currentOptions[i])) {

				if (i + 1 < currentOptions.length
						&& !possibleParameters.contains(currentOptions[i + 1])
						&& !possibleFlags.contains(currentOptions[i + 1])) {

					if (!parameters.get(noPrefixOption).isSet()) {

						
						parameters.get(noPrefixOption).setValue(
								currentOptions[i + 1]);
						

						parameterArray.add(currentOptions[i]);
						parameterArray.add(currentOptions[i + 1]);
						i++;
					}
					// option known, but already read and set
					else {
						unexpectedParameters.add(currentOptions[i]);
						unexpectedParameters.add(currentOptions[i + 1]);
						i++;
					}
				}
				// no option-value following the option - or the following
				// String is known as parameter or flag
				else {
					throw new NoParameterValueException("Parameter "
							+ currentOptions[i]
							+ " requires an parameter-value!");
				}
			}
			// flag
			else if (possibleFlags.contains(currentOptions[i])
					&& !parameters.get(noPrefixOption).isSet()) {
				// option-value is following
				if (i + 1 < currentOptions.length
						&& !currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
					throw new NoParameterValueException("Parameter "
							+ currentOptions[i]
							+ " requires no parameter-value! "
							+ "(read parameter-value: " + currentOptions[i + 1]
							+ ")");
				}

				parameters.get(noPrefixOption).setValue(Flag.SET);

				parameterArray.add(currentOptions[i]);
			}
			// flag not known or flag known but already set
			else {
				unexpectedParameters.add(currentOptions[i]);
				if (i + 1 < currentOptions.length
						&& !currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
					unexpectedParameters.add(currentOptions[i + 1]);
					i++;
				}
			}
		}
		currentParameters = new String[parameterArray.size()];
		currentParameters = parameterArray.toArray(currentParameters);
		String[] remain = new String[unexpectedParameters.size()];
		unexpectedParameters.toArray(remain);

//		if (this.debug) {
//			for (Map.Entry<String, Option> option : parameters.entrySet()) {
//				debugFine("option " + option.getKey() + " has value "
//						+ option.getValue().getValue());
//
//			}
//		}

		return remain;
	}

	/**
	 * Returns the value of the given option, if there is one.
	 * 
	 * @param option
	 *            option to get value of. The option should be asked for without
	 *            leading &quot;-&quot; or closing &quot;:&quot;.
	 * @return String value of given option
	 * @throws UnusedParameterException
	 *             if the given option is not used
	 * @throws NoParameterValueException
	 *             if the given option is only a flag and should therefore have
	 *             no value
	 */
	public String getOptionValue(String option)
			throws UnusedParameterException, NoParameterValueException {

		if (parameters.containsKey(option)) {
			try {
				return parameters.get(option).getValue();
			} catch (ClassCastException e) {
				throw new NoParameterValueException("Parameter " + option
						+ " is flag which has no value!", e);
			}
		} else {
			throw new UnusedParameterException("Parameter " + option
					+ " is not specified!");
		}
	}

	public Option getOption(String name)throws UnusedParameterException{
		if(parameters.containsKey(name)){
			return parameters.get(name);
		}
		throw new UnusedParameterException("Parameter "+name+ " is not specified!");
	}
	
	/**
	 * Returns true if the value of the given option is set, false otherwise.
	 * 
	 * @param option
	 *            The option should be asked for without leading &quot;-&quot;
	 *            or closing &quot;:&quot;.
	 * @return boolean true if the value of the given option is set, false otherwise
	 */
	public boolean isSet(String option) {
		if (parameters.containsKey(option)) {
			return parameters.get(option).isSet();
		}
		return false;
	}

	/**
	 * Returns an usage-String according to the descriptions given in the
	 * constructor. Same as <code>usage(message,true)</code>.
	 * 
	 * @param message
	 *            some error-message, if needed (may be null or empty String)
	 * @return String an usage-String according to the descriptions given in the
	 *         constructor.
	 */
	public String usage(String message) {
		return usage(message, true);
	}

	/**
	 * Returns an usage-String according to the descriptions given in the
	 * constructor.
	 * 
	 * @param message
	 *            some error-message, if needed (may be null or empty String)
	 * @param standalone
	 *            whether the class using this OptionHandler provides a main
	 *            method
	 * @return String an usage-String according to the descriptions given in the
	 *         constructor.
	 */
	public String usage(String message, boolean standalone) {
		String empty = "";
		String space = " ";
		int lineLength = 80;
		String paramLineIndent = "        ";
		StringBuffer messageBuffer = new StringBuffer();
		if (!(message == null || message.equals(empty))) {
			messageBuffer.append(message).append(NEWLINE);
		}

		String[] options = new String[parameters.size()];

		String[] shortDescriptions = new String[options.length];

		String[] longDescriptions = new String[options.length];

		int longestShortline = 0;
		StringBuffer paramLine = new StringBuffer();
		int currentLength = programCall.length();

		int counter = 0;
		for (Map.Entry<String, Option> option : parameters.entrySet()) {

			String currentOption = option.getKey();

			String desc = option.getValue().getDescription();

			String shortDescription = empty;
			String longDescription = desc;

			if (option.getValue() instanceof Parameter) {
				shortDescription = desc.substring(desc.indexOf("<"), desc
						.indexOf(">") + 1);
				longDescription = desc.substring(desc.indexOf(">") + 1);
				currentOption = currentOption.substring(0);
			}
			currentOption = OPTION_PREFIX + currentOption;
			options[counter] = currentOption;
			shortDescriptions[counter] = shortDescription;
			longDescriptions[counter] = longDescription;
			longestShortline = Math.max(longestShortline, currentOption
					.length()
					+ shortDescription.length() + 1);
			currentLength = currentLength + currentOption.length() + 2
					+ shortDescription.length();
			if (currentLength > lineLength) {
				paramLine.append(NEWLINE);
				paramLine.append(paramLineIndent);
				currentLength = paramLineIndent.length();
			}
			paramLine.append(currentOption);
			paramLine.append(space);
			paramLine.append(shortDescription);
			paramLine.append(space);

			counter++;
		}

		String mark = " : ";
		String indent = "  ";
		int firstCol = indent.length() + longestShortline;
		int secondCol = mark.length();
		StringBuffer descriptionIndent = new StringBuffer();
		for (int i = 0; i < firstCol + secondCol; i++) {
			descriptionIndent.append(space);
		}
		int thirdCol = lineLength - (firstCol + secondCol);
		int[] cols = { firstCol, secondCol, thirdCol };
		PrettyPrinter prettyPrinter = new PrettyPrinter(cols, empty);
		char fillchar = ' ';

		if (standalone) {
			messageBuffer.append("Usage: ");
			messageBuffer.append(NEWLINE);
		}
		messageBuffer.append(programCall);
		if (standalone) {
			messageBuffer.append(space);
			messageBuffer.append(paramLine);
		}
		messageBuffer.append(NEWLINE);

		for (int i = 0; i < options.length; i++) {
			StringBuffer option = new StringBuffer();
			option.append(indent);
			option.append(options[i]);
			option.append(space);
			option.append(shortDescriptions[i]);
			Vector<String> lines = prettyPrinter.breakLine(longDescriptions[i],
					2);
			String[] firstline = { option.toString(), mark,
					lines.firstElement() };
			messageBuffer.append(
					prettyPrinter.formattedLine(firstline, fillchar)).append(
					NEWLINE);
			for (int l = 1; l < lines.size(); l++) {
				messageBuffer.append(descriptionIndent).append(lines.get(l))
						.append(NEWLINE);
			}
		}
		return messageBuffer.toString();
	}

	/**
	 * Returns a copy of the parameter array as given to the last call of
	 * {@link #grabOptions(String[]) grabOptions(String[])}. The resulting
	 * array will contain only those values that were recognized and needed by
	 * this OptionHandler.
	 * 
	 * @return a copy of the parameter array as given to the last call of
	 *         {@link #grabOptions(String[]) grabOptions(String[])}
	 */
	public String[] getParameterArray() {
		String[] parameterArray = new String[currentParameters.length];
		System.arraycopy(currentParameters, 0, parameterArray, 0,
				currentParameters.length);
		return parameterArray;
	}

	/**
	 * Adds the given parameter map to the OptionHandler's current parameter map.
	 * 
	 * @param params Parameter map to be added.
	 */
	public void put(Map<String, Option> params) {
		this.parameters.putAll(params);
	}

	/**
	 * Adds the given option to the OptionHandler's current parameter map.
	 * 
	 * @param option Option to be added.
	 */
	public void put(Option option) {
		Option put = this.parameters.put(option.getName(), option);
		if(put != null){
			warning("Parameter "+option.getName()+" has been already set before! Old value has been overwritten!");
		}
	}

	/**
	 * Adds the given option with the given name to the OptionHandler's current parameter map.
	 * 
	 * @param name The name of the option to be added.
	 * @param option The option to be added.
	 */
	public void put(String name, Option option) {
		Option put = this.parameters.put(name, option);
		if(put != null){
			warning("Parameter "+name+" has been already set before! Old value has been overwritten!");
		}
	}

	
	public void setGlobalParameterConstraint(GlobalParameterConstraint gpc){
		globalParameterConstraints.add(gpc);
	}
	
	/**
	 * Sets the OptionHandler's programmCall (@link #programCall} to the given call.
	 * 
	 * @param call The new programm call.
	 */
	public void setProgrammCall(String call) {
		programCall = call;
	}

	/**
	 * Removes the given option from the OptionHandler's parameter map.
	 * 
	 * @param optionName Option to be removed.
	 * @throws UnusedParameterException If there is no such option. 
	 */
	public void remove(String optionName) throws UnusedParameterException{
		Option removed = this.parameters.remove(optionName);
		if (removed == null) {
			throw new UnusedParameterException("Cannot remove parameter "+optionName+"! Parameter has not been set before!");
		}
	}
	
	public Option[] getOptions(){
		
		return  parameters.values().toArray(new Option[]{});
	}
}
