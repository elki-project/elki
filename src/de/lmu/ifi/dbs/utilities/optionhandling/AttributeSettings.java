package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;
import java.util.ArrayList;

/**
 * Encapsulates the current settings of the attributes of an object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class AttributeSettings {
  /**
   * The settings of the attributes.
   */
  private List<Setting> settings;

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
    this.settings = new ArrayList<Setting>();
  }

  public void addSetting(String name,String value) {
    settings.add(new Setting(name, value));
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString(String prefix) {
    String result = prefix + object.getClass().getSimpleName() + ":";

    for (Setting s: settings) {
      result += "\n" + prefix + s;
    }
    return result;
  }

  /**
   * Encapsulates a setting of one attribute.
   */
  private class Setting {
    /**
     * The name of the attribute.
     */
    String name;

    /**
     * The value of the attribute.
     */
    String value;

    /**
     * Creates a new setting object.
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public Setting(String name, String value) {
      this.name = name;
      this.value = value;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
     return OptionHandler.OPTION_PREFIX + name + " " + value;
    }
  }
}
