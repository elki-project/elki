package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class VectorParameter extends ListParameter<List> {

	public VectorParameter(String name, String description) {
		super(name, description);
		inputField = createInputField();
	}

	private JComponent createInputField() {
		JTextField field = new JTextField();
		field.setColumns(30);
		return field;
	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

	@Override
	public Component getInputField() {
		return inputField;
	}

	@Override
	public String getValue() {

		StringBuffer buffer = new StringBuffer();
		for (List<Double> vectors : value) {

			for (int i = 0; i < vectors.size(); i++) {

				buffer.append(vectors.get(i));
				if (i == vectors.size() - 1) {
					buffer.append(VECTOR_SPLIT);
				} else {
					buffer.append(SPLIT);
				}
			}
		}

		return buffer.toString();
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		String[] vectors = VECTOR_SPLIT.split(value);
		if (vectors.length == 0) {
			// TODO
			throw new WrongParameterValueException("");
		}
		ArrayList<List> vecs = new ArrayList<List>();

		int firstLength = -1;
		for (int c = 0; c < vectors.length; c++) {
			String[] coordinates = SPLIT.split(vectors[c]);
			if (c == 0) {
				firstLength = coordinates.length;
			} else if (coordinates.length != firstLength) {
				throw new WrongParameterValueException("");
			}

			ArrayList<Double> vectorCoord = new ArrayList<Double>();
			for (int d = 0; d < coordinates.length; d++) {
				try {
					vectorCoord.add(Double.parseDouble(coordinates[d]));
				} catch (NumberFormatException e) {
					throw new WrongParameterValueException("");
				}
			}
			vecs.add(vectorCoord);
		}
		this.value = vecs;

	}

	@Override
	public void setValue() throws ParameterException {

		setValue(((JTextField) inputField).getText());
	}
	
	public int[] vectorSizes(){
		
		int[] sizes = new int[getListSize()];
		
		int i = 0;
		for(List vecs : value){
			sizes[i] = vecs.size();
			i++;
		}
		
		return sizes;
	}

}
