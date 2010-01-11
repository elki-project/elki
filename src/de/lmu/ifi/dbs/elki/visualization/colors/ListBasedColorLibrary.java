package de.lmu.ifi.dbs.elki.visualization.colors;


/**
 * Color library using the color names from a list.
 * 
 * @author Erich Schubert
 */
public class ListBasedColorLibrary implements ColorLibrary {
  /**
   * Array of color names.
   */
  private String[] colors;

  /**
   * Color scheme name
   */
  private String name;

  /**
   * Constructor without a properties file name.
   */
  public ListBasedColorLibrary(String[] colors, String name) {
    this.colors = colors;
    this.name = name;
  }

  @Override
  public String getColor(int index) {
    return colors[index % colors.length];
  }

  @Override
  public int getNumberOfNativeColors() {
    return colors.length;
  }

  /**
   * Get the color scheme name.
   * 
   * @return the name
   */
  protected String getName() {
    return name;
  }
}