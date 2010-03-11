package de.lmu.ifi.dbs.elki.properties;

/**
 * Provides a pair of entry of a property and the proper description.
 * 
 * @author Arthur Zimek
 */
public class PropertyDescription {
  /**
   * Entry of property.
   */
  private String entry;

  /**
   * Description of property.
   */
  private String description;

  /**
   * Provides a pair of entry of a property and the proper description.
   * 
   * @param entry entry of property
   * @param description description of property
   */
  public PropertyDescription(String entry, String description) {
    this.entry = entry;
    this.description = description;
  }

  /**
   * Returns the description of the property.
   * 
   * 
   * @return the description of the property
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the entry of the property.
   * 
   * 
   * @return the entry of the property
   */
  public String getEntry() {
    return entry;
  }
}