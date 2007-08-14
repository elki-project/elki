package de.lmu.ifi.dbs.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * Class to moden an editable object.
 * 
 * @author Steffi Wanka
 * 
 */
public class EditObject {

	private Class<?> type;

	// private String[] parameters;

	private Object editObject;

	private Map<String, String> optionToValue;

	/**
	 * Creates an EditObject with the specified class-type.
	 * 
	 * @param type
	 *            the class of the EditObject
	 */
	public EditObject(Class<?> type) {
		this.type = type;
	}

	/**
	 * Creates the actual edit object associated with the given class name.
	 * Creates a new edit object instance of the class represented by the class
	 * object associated with the class with the given string name.
	 * 
	 * @param className
	 *            the class name associated with this edit object
	 */
	public void setEditObjectClass(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException, UnableToComplyException {

		/*
		 * Possible options: - className is null - className has been set before -
		 * first setting of className
		 */

		// try to instantiate edit object
		setEditObject(Class.forName(className).newInstance());
	}

	/**
	 * Sets the actual edit object.
	 * 
	 * @param obj
	 *            the edit object to be set
	 */
	private void setEditObject(Object obj) throws UnableToComplyException {

		if (!type.isAssignableFrom(obj.getClass())) {
			throw new UnableToComplyException("Type " + this.type.getName() + " is not assignable from the given object.");
		}

		this.editObject = obj;
		this.optionToValue = new HashMap<String, String>();
		if (isParameterizable()) {

			for (Option<?> o : ((Parameterizable) editObject).getPossibleOptions()) {
				optionToValue.put(o.getName(), null);
			}
		}
	}

	/**
	 * Returns the class name of the actual edit object of this EditObject.
	 * 
	 * @return the class name of the actual edit object, or an empty string if
	 *         there is none.
	 */
	public String getClassName() {
		if (editObject == null) {
			return "";
		}
		return editObject.getClass().getName();
	}

	/**
	 * Returns true if this EditObject is parameterizable, false otherwise.
	 * 
	 * @return true if this EditObject is parameterizable, false otherwise.
	 */
	public boolean isParameterizable() {
		return (editObject instanceof Parameterizable);
	}

	/**
	 * Returns the possible options of this EditObject if it is parameterizable.
	 * If it is not the method returns null.
	 * 
	 * @return the possible options of this EditObject if it is parameterizable,
	 *         null otherwise
	 */
	public Option<?>[] getOptions() {

		if (!isParameterizable()) {
			return null;
		}
		return ((Parameterizable) editObject).getPossibleOptions();
	}

	/**
	 * Returns the class name of the actual edit object of this EditObject.
	 * 
	 * @return the class name of the actual edit object
	 */
	public String getName() {
		return editObject.getClass().getName();
	}

	/**
	 * Returns the description of this EditObject if it is parameterizable. If
	 * it is not the method returns an empty string.
	 * 
	 * @return
	 */
	public String getDescription() {
		if (isParameterizable()) {
			return ((Parameterizable) editObject).description();
		}
		return "";
	}

	/**
	 * If this EditObject is an instance of {@link Algorithm} the method returns
	 * the description of the algorithm. If it is not an empty string is
	 * returned.
	 * 
	 * @return
	 */
	public String getAlgorithmInfo() {
		if (isAlgorithm()) {
			return ((Algorithm) editObject).getDescription().toString();
		}
		return "";
	}

	/**
	 * Returns true if this EditObject is an instance of {@link Algorithm},
	 * false otherwise.
	 * 
	 * @return true if this EditObject is an instance of {@link Algorithm},
	 *         false otherwise.
	 */
	public boolean isAlgorithm() {
		return editObject instanceof Algorithm;
	}

	/**
	 * Checks if all global constraints are kept. If so, the method returns
	 * true, otherwise false.
	 * 
	 * @throws ParameterException
	 *             if any of the global constraints are violated.
	 */
	public void checkGlobalConstraints() throws ParameterException {
		if (isParameterizable()) {
			((Parameterizable) editObject).checkGlobalParameterConstraints();
		}
	}

	/**
	 * Checks if this EditObject is valid.
	 * <p>
	 * If this EditObject is parameterizable the methods checks if all option
	 * values set are valid and if no global parameter constraints are violated.
	 * If so, the method returns true, otherwise false.
	 * 
	 * </p>
	 * 
	 * @return true, if this EditObject has valid option values, false otherwise
	 * @throws ParameterException
	 *             thrown if a global parameter constraint is violated
	 */
	public boolean isValid() throws ParameterException {
		if (isParameterizable()) {
			for (Option<?> o : ((Parameterizable) editObject).getPossibleOptions()) {
				if (o instanceof Flag) {
					continue;
				}
				if (o instanceof Parameter && !((Parameter<?, ?>) o).isOptional() && !o.isSet()) {
					return false;
				}
			}
			((Parameterizable) editObject).checkGlobalParameterConstraints();
		}
		return true;
	}

	/**
	 * If this EditObject is parameterizabel, the method sets the option value
	 * for the given option in the {@link #optionToValue}-Map of this
	 * EditObject. Otherwise, the method doesn't do anything.
	 * 
	 * 
	 * @param optionName
	 *            the option whose value is to be set
	 * @param optionValue
	 *            the value to be set
	 * @param verify
	 *            flag to indicate if also the value of the actual option of the
	 *            parameterizable edit object is to be set
	 * @throws ParameterException
	 *             if the given option does not exist
	 */
	public void setOptionValue(String optionName, String optionValue, boolean verify) throws ParameterException {
		if (isParameterizable()) {
			if (!optionToValue.containsKey(optionName)) {
				throw new UnusedParameterException("Parameterizable-Object " + editObject.toString() + " has no parameter " + optionValue);
			}
			if (verify) {
				for (Option<?> o : ((Parameterizable) editObject).getPossibleOptions()) {
					if (o.getName().equals(optionName)) {
						o.setValue(optionValue);
					}
				}
			}
			optionToValue.put(optionName, optionValue);
		}
	}

	/**
	 * Returns a string representation of the parameters and their values of
	 * this EditObject. A parameter and its value is represented as follows:
	 * -option value, a set flag is represented as -flag. If this EditObject is
	 * not parameterizable the method returns an empty string.
	 * 
	 * @return a string representation of the parameters and their values
	 */
	public String parameterValuesAsString() {

		StringBuilder bob = new StringBuilder();
		if (isParameterizable()) {
			int i = 0;
			int size = ((Parameterizable) editObject).getPossibleOptions().length;
			for (Option<?> o : ((Parameterizable) editObject).getPossibleOptions()) {

				if (o.isSet()) {
					bob.append("-" + o.getName());
				} else {
					continue;
				}
				if (o instanceof Parameter) {
					// bob.append(" " + o.getValue().toString());
					bob.append(" " + optionToValue.get(o.getName()));
				}

				if (++i != size) {
					bob.append(" ");
				}
			}
		}
		return bob.toString();
	}

	public String[] parameterValuesToArray() {
		Vector<String> p = new Vector<String>();
		if (isParameterizable()) {
		
			for (Option<?> o : ((Parameterizable) editObject).getPossibleOptions()) {

				if (o.isSet()) {
					p.add("-" + o.getName());
				} else {
					continue;
				}
				if (o instanceof Parameter) {
					// bob.append(" " + o.getValue().toString());
					p.add(optionToValue.get(o.getName()));
				}
			}
		}
		return p.toArray(new String[]{});
	}

}
