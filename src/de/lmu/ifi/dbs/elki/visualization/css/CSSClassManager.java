package de.lmu.ifi.dbs.elki.visualization.css;

import java.util.Collection;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;


/**
 * Manager class to track CSS classes used in a particular SVG document.
 * 
 * @author Erich Schubert
 */
public class CSSClassManager {
  /**
   * Store the contained CSS classes.
   */
  private HashMap<String, CSSClass> store = new HashMap<String, CSSClass>();
  
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 8807736974456191901L;

  /**
   * Add a single class to the map.
   * 
   * @param clss
   * @return existing (old) class
   * @throws CSSNamingConflict when a class of the same name but different owner object exists.
   */
  public CSSClass addClass(CSSClass clss) throws CSSNamingConflict {
    CSSClass existing = store.get(clss.getName());
    if (existing != null && existing.getOwner() != null && existing.getOwner() != clss.getOwner()) {
      throw new CSSNamingConflict("CSS class naming conflict between "+clss.getOwner().toString()+" and "+existing.getOwner().toString());
    }
    return store.put(clss.getName(), clss);
  }

  /**
   * Remove a single CSS class from the map.
   * Note that classes are removed by reference, not by name!
   * 
   * @param clss Class to remove
   */
  public synchronized void removeClass(CSSClass clss) {
    CSSClass existing = store.get(clss.getName());
    if (existing == clss) {
      store.remove(existing.getName());
    }
  }
  
  /**
   * Retrieve a single class by name and owner
   * 
   * @param name
   * @param owner
   * @return existing (old) class
   * @throws CSSNamingConflict if an owner was specified and doesn't match
   */
  public CSSClass getClass(String name, Object owner) throws CSSNamingConflict {
    CSSClass existing = store.get(name);
    // Not found.
    if (existing == null) {
      return null;
    }
    // Different owner
    if (owner != null && existing.getOwner() != owner) {
      throw new CSSNamingConflict("CSS class naming conflict between "+owner.toString()+" and "+existing.getOwner().toString());
    }
    return existing;
  }
  
  /**
   * Retrieve a single class by name only
   * 
   * @param name
   * @return existing (old) class
   */
  public CSSClass getClass(String name) {
    return store.get(name);
  }
  
  /**
   * Check if a name is already used in the classes.
   * 
   * @param name
   * @return true if the class name is already used.
   */
  public boolean contains(String name) {
    return store.containsKey(name);
  }

  /**
   * Serialize managed CSS classes to rule file.
   * 
   * @param buf String buffer
   */
  public void serialize(StringBuffer buf) {
    for (CSSClass clss : store.values()) {
      clss.appendCSSDefinition(buf);
    }
  }
  
  /**
   * Get all CSS classes in this manager.
   * 
   * @return CSS classes.
   */
  public Collection<CSSClass> getClasses() {
    return store.values();
  }
  
  /**
   * Check compatibility
   */
  public boolean testMergeable(CSSClassManager other) {
    for (CSSClass clss : other.getClasses()) {
      CSSClass existing = store.get(clss.getName());
      // Check for a naming conflict.
      if (existing != null && existing.getOwner() != null && clss.getOwner() != null && existing.getOwner() != clss.getOwner()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Merge CSS classes, for example to merge two plots.
   * 
   * @throws CSSNamingConflict If there is a naming conflict.
   */
  public boolean mergeCSSFrom(CSSClassManager other) throws CSSNamingConflict {
    for (CSSClass clss : other.getClasses()) {
      this.addClass(clss);
    }
    return true;
  }
  
  /**
   * Class to signal a CSS naming conflict.
   * 
   */
  public class CSSNamingConflict extends Exception {
    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 4163822727195636747L;
    
    /**
     * Exception to signal a CSS naming conflict.
     * 
     * @param msg Exception message
     */
    public CSSNamingConflict(String msg) {
      super(msg);
    }
  }

  /**
   * Update the text contents of an existing style element.
   * 
   * @param document Document element (factory)
   * @param style Style element
   */
  public void updateStyleElement(Document document, Element style) {
    StringBuffer buf = new StringBuffer();
    serialize(buf);
    Text cont = document.createTextNode(buf.toString());
    while (style.hasChildNodes()) {
      style.removeChild(style.getFirstChild());
    }
    style.appendChild(cont);
  }
  
  /**
   * Make a (filled) CSS style element for the given document.
   * 
   * @param document Document
   * @return Style element
   */
  public Element makeStyleElement(Document document) {
    Element style = SVGUtil.makeStyleElement(document);
    updateStyleElement(document, style);
    return style;
  }
}
