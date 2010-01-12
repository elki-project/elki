package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Encapsulates a setting of one attribute.
 */
@Deprecated
// TODO: remove
public class AttributeSetting {
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
   * 
   * @param name the name of the attribute
   * @param value the value of the attribute
   */
  public AttributeSetting(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Returns the name of the attribute
   * 
   * @return the name of the attribute
   */
  public String getName() {
    return name;
  }

  /**
   * Returns a string representation of the value of the attribute
   * 
   * @return a string representation of the value of the attribute
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return OptionHandler.OPTION_PREFIX + name + " " + value;
  }
}