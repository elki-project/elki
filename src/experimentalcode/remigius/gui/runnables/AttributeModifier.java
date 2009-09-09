package experimentalcode.remigius.gui.runnables;

import org.w3c.dom.Element;

/**
 * Runnable wrapper for modifying XML-Attributes. 
 * 
 * @author Remigius Wojdanowski
 *
 */
public class AttributeModifier implements Runnable {
  
  /**
   * Provides the attribute to be modified.
   */
  private Element e;
  
  /**
   * The name of the attribute to be modified.
   */
  private String attribute;
  
  /**
   * The new value of the attribute.
   */
  private String newValue;
  
  /**
   * Trivial constructor.
   * 
   * @param e provides the attribute to be modified.
   * @param attribute the name of the attribute to be modified.
   * @param newValue the new value of the attribute.
   */
  public AttributeModifier(Element e, String attribute, String newValue){
    this.e = e;
    this.attribute = attribute;
    this.newValue = newValue;
  }
  
  @Override
  public void run() {
    e.setAttribute(attribute, newValue);
  }
}
