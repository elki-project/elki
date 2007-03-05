package de.lmu.ifi.dbs.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

public class EditObject implements EditObjectChangeListener{
	
	
	private Class type;
	
	private String parameters;
	
	private Object editObject;
	
	private CustomizerPanel customizer;
	
	private Vector<EditObjectChangeListener> listener;
	
	public EditObject(Class type){
		this.type = type;
		this.listener = new Vector<EditObjectChangeListener>();
	}
	
	public void setEditObjectClass(String className){
		
		/* Possible options:
		 * - className is null
		 * - className has been set before
		 * - first setting of className
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
	
	private void setEditObject(Object obj){
		
		// TODO also check if the object is maybe a subclass of the wanted class 
		if (!type.isAssignableFrom(obj.getClass())) {
			System.err.println("object is not of correct type!");
			return;
		}

		this.editObject = obj;
		react();

	}
	
	private void react(){
		
		// is it parameterizable??
		if(editObject instanceof Parameterizable){
			//show customizer panel to specify parameters
			
			Option[] options = ((Parameterizable)editObject).getPossibleOptions();
			for(Option o : options){
				try {
					System.out.println("option "+o.getName()+": "+o.getValue());
				} catch (UnusedParameterException e) {
					
				}
			}
			
			customizer = new CustomizerPanel((Parameterizable) editObject);
//			customizer.addPropertyChangeListener(this);
			customizer.addEditObjectChangeListener(this);
			customizer.setVisible(true);
		}
		else{
			this.parameters = "";
		}
	}
	
	public String toString(){
		
		if(editObject == null){
			return "";
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(editObject.getClass().getName());
		builder.append(" ");
		builder.append(parameters);
		
		return builder.toString();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		
		//update parameters
		parameters = customizer.getParameterValuesAsString();
		
		Option[] options = ((Parameterizable)editObject).getPossibleOptions();
		for(Option o : options){
			try {
				System.out.println("option "+o.getName()+": "+o.getValue());
			} catch (UnusedParameterException e) {
				System.out.println("option "+o.getName()+": null");
			}
		}
		System.out.println("property: "+evt.getPropertyName());
		
	}
	
	public String getClassName(){
		
		if(editObject == null){
			return "";
		}
		
		return editObject.getClass().getName();
	}
	
	public String getParameters(){
		return parameters;
	}

	public void editObjectChanged() {
		
		parameters = customizer.getParameterValuesAsString();
		fireEditObjectChanged();
	}
	
	public void addEditObjectListener(EditObjectChangeListener l){
		listener.add(l);
	}
	
	private void fireEditObjectChanged(){
		for(EditObjectChangeListener l : listener){
			l.editObjectChanged();
		}
	}

}
