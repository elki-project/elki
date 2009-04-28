package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the current settings of the attributes of an object.
 * 
 * @author Elke Achtert
 */
public class AttributeSettings {
  /**
   * The settings of the attributes.
   */
  private List<AttributeSetting> settings;

  /**
   * The object.
   */
  private Object object;

  /**
   * Creates a new parameter setting object.
   * 
   * @param object the object
   */
  public AttributeSettings(Object object) {
    this.object = object;
    this.settings = new ArrayList<AttributeSetting>();
  }

  /**
   * Adds a new setting to this settings.
   * 
   * @param name the name of the attribute
   * @param value a string representation of the value of the attribute
   */
  public void addSetting(String name, String value) {
    settings.add(new AttributeSetting(name, value));
  }

  /**
   * Add an option to the settings list.
   * 
   * @param option Option to add
   */
  public void addOption(Option<?> option) {
    if(option instanceof Flag) {
      addSetting(option.getName(), Boolean.toString(option.isSet()));
    }
    else {
      Object value;
      try {
        value = option.getValue();
      }
      catch(UnusedParameterException e) {
        value = null;
      }
      if(value != null) {
        addSetting(option.getName(), value.toString());
      }
      else {
        addSetting(option.getName(), "null");
      }
    }
  }

  /**
   * Returns the list of settings.
   * 
   * @return the list of settings
   */
  public List<AttributeSetting> getSettings() {
    return settings;
  }

  /**
   * Returns a string representation of the object.
   * 
   * @param prefix
   * @return a string representation of the object.
   */
  public String toString(String prefix) {
    String result = prefix + object.getClass().getSimpleName() + ":";

    for(AttributeSetting s : settings) {
      result += "\n" + prefix + s;
    }
    return result;
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return toString("");
  }

}
