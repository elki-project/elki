package de.lmu.ifi.dbs.gui;

import java.util.Vector;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

public class EditObject {

	private Class<?> type;

	private String[] parameters;

	private Object editObject;

	private Vector<EditObjectChangeListener> listener;

	public EditObject(Class<?> type) {
		this.type = type;
		this.listener = new Vector<EditObjectChangeListener>();
	}

	public void setEditObjectClass(String className) {

		/*
		 * Possible options: - className is null - className has been set before -
		 * first setting of className
		 */

		// try to instantiate edit object
		try {
			setEditObject(Class.forName(className).newInstance());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setEditObject(Object obj) {

		// TODO also check if the object is maybe a subclass of the wanted class
		if (!type.isAssignableFrom(obj.getClass())) {
			System.err.println("object is not of correct type!");
			return;
		}

		this.editObject = obj;

		if (!isParameterizable()) {
			parameters = new String[0];
		}
	}

	public String toString() {

		if (editObject == null) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(editObject.getClass().getName());
		builder.append(" ");
		builder.append(parameters);

		return builder.toString();
	}

	public String getClassName() {

		if (editObject == null) {
			return "";
		}

		return editObject.getClass().getName();
	}

	public String[] getParameters() {
		return parameters;
	}

	public void addEditObjectListener(EditObjectChangeListener l) {
		listener.add(l);
	}

	private void fireEditObjectChanged() {

		for (EditObjectChangeListener l : listener) {
			l.editObjectChanged(getClassName(), parameters);
		}
	}

	public void updateParameters(String[] parameters) {

		this.parameters = parameters;

		fireEditObjectChanged();

	}

	public boolean isParameterizable() {
		return (editObject instanceof Parameterizable);
	}

	public Option<?>[] getOptions() {

		if (!isParameterizable()) {
			return null;
		}
		return ((Parameterizable) editObject).getPossibleOptions();
	}

	public String getName() {
		return editObject.getClass().getName();
	}

	public String getDescription() {
		if (isParameterizable()) {
			return ((Parameterizable) editObject).description();
		}
		return "";
	}

	public String getAlgorithmInfo() {
		if (isAlgorithm()) {
			return ((Algorithm) editObject).getDescription().toString();
		}
		return "";
	}

	public boolean isAlgorithm() {
		return editObject instanceof Algorithm;
	}

	public void checkGlobalConstraints() throws ParameterException {
		if (isParameterizable()) {
			((Parameterizable) editObject).checkGlobalParameterConstraints();
		}
	}

}
