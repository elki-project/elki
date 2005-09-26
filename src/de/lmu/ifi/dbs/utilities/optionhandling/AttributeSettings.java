package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the current settings of the attributes of an object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
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
   * @param object          the object
   */
  public AttributeSettings(Object object) {
    this.object = object;
    this.settings = new ArrayList<AttributeSetting>();
  }

  /**
   * Adds a new setting to this settings.
   * @param name the name of the attribute
   * @param value a string representation of the value of the attribut
   */
  public void addSetting(String name,String value) {
    settings.add(new AttributeSetting(name, value));
  }

  /**
   * Returns the list of settings.
   * @return the list of settings
   */
  public List<AttributeSetting> getSettings() {
    return settings;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString(String prefix) {
    String result = prefix + object.getClass().getSimpleName() + ":";

    for (AttributeSetting s: settings) {
      result += "\n" + prefix + s;
    }
    return result;
  }


}
