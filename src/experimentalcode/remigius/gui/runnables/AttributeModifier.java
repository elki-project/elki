package experimentalcode.remigius.gui.runnables;

import org.w3c.dom.Element;

public class AttributeModifier implements Runnable {
  
  private Element e;
  private String attribute;
  private String newValue;
  
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
