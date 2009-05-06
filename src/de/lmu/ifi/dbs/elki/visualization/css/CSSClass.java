package de.lmu.ifi.dbs.elki.visualization.css;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class representing a single CSS class.
 * 
 * @author Erich Schubert
 */
public class CSSClass {
  /**
   * CSS class name
   */
  private String name;
  
  /**
   * Actual CSS statements
   */
  private Collection<Pair<String, String>> statements;
  
  /**
   * Owner.
   */
  private WeakReference<Object> owner;
  
  /**
   * Full constructor
   * 
   * @param owner Class owner (to detect conflicts)
   * @param name Class name
   * @param statements Collection of CSS statements
   */
  public CSSClass(Object owner, String name, Collection<Pair<String, String>> statements) {
    this.owner = new WeakReference<Object>(owner);
    this.name = name;
    this.statements = statements;
    if (!checkName(name)) {
      throw new InvalidCSS("Given name is not a valid CSS class name.");
    }
    if (this.statements != null) {
      if (!checkCSSStatements(this.statements)) {
        throw new InvalidCSS("Invalid statement in CSS class "+name);
      }
    } else {
      // if needed, use an array list.
      this.statements = new ArrayList<Pair<String,String>>();
    }
  }
  
  /**
   * Simplified constructor, empty statments list.
   * 
   * @param owner Class owner.
   * @param name Class name.
   */
  public CSSClass(Object owner, String name) {
    this(owner, name, null);
  }
  
  /**
   * Verify that the name is an admissible CSS class name.
   * 
   * TODO: implement.
   * 
   * @param name name to use
   * @return true if valid CSS class name
   */
  public static boolean checkName(String name) {
    // TODO: implement a sanity check - regexp?
    return (name != null);
  }

  /**
   * Return a sanitized version of the given string.
   * 
   * TODO: implement extensive checks.
   * 
   * @param name name to sanitize
   * @return Sanitized version.
   */
  public static String sanitizeName(String name) {
    // TODO: implement a sanitization - regexp?
    return name;
  }

  /**
   * Validate a single CSS statement.
   * 
   * TODO: implement extensive checks.
   * 
   * @param key Key
   * @param value Value
   * @return true if valid statement.
   */
  public static boolean checkCSSStatement(String key, String value) {
    // TODO: implement more extensive checks!
    return (key != null) && (value != null);
  }

  /**
   * Validate a set of CSS statements.
   * 
   * TODO: checks are currently not very extensive.
   * 
   * @param statements
   * @return true if valid
   */
  public static boolean checkCSSStatements(Collection<Pair<String,String>> statements) {
    for (Pair<String, String> pair : statements) {
      if (!checkCSSStatement(pair.getFirst(), pair.getSecond())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the class name.
   * 
   * @return class name.
   */
  public String getName() {
    return this.name;
  }
  
  /**
   * Set the class name.
   * 
   * @param name new class name.
   */
  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * Get class owner.
   * 
   * @return class owner. 
   */
  public Object getOwner() {
    return this.owner.get();
  }

  /**
   * Get the current value of a particular CSS statement.
   * 
   * @param key statement key.
   * @return current value or null.
   */
  public String getStatement(String key) {
    for (Pair<String, String> pair : statements) {
      if (pair.getFirst().equals(key)) {
        return pair.getSecond();
      }
    }
    return null;
  }
  
  /**
   * Set a CSS statement.
   * 
   * @param key Statement key.
   * @param value Value or null (to unset)
   */
  public void setStatement(String key, String value) {
    if (value != null) {
      if (!checkCSSStatement(key, value)) {
        throw new InvalidCSS("Invalid CSS statement.");
      }
    }
    for (Pair<String, String> pair : statements) {
      if (pair.getFirst().equals(key)) {
        if (value != null) {
          pair.setSecond(value);
        } else {
          statements.remove(pair);
        }
        return;
      }
    }
    if (value != null) {
      statements.add(new Pair<String, String>(key, value));
    }
  }

  /**
   * Remove a CSS statement.
   * 
   * @param key Statement key.
   */
  public void removeStatement(String key) {
    setStatement(key, null);
  }
  
  /**
   * Append CSS definition to a stream
   * 
   * @param buf String buffer to append to.
   */
  public void appendCSSDefinition(StringBuffer buf) {
    buf.append("\n.");
    buf.append(name);
    buf.append("{");
    for (Pair<String, String> pair : statements) {
      buf.append(pair.getFirst());
      buf.append(":");
      buf.append(pair.getSecond());
      buf.append(";\n");
    }
    buf.append("}\n");
  }
  
  /**
   * Exception class thrown when encountering invalid CSS.
   */
  public class InvalidCSS extends RuntimeException {
    /**
     * Constructor. See {@link RuntimeException}.
     * 
     * @param msg Error message.
     */
    public InvalidCSS(String msg) {
      super(msg);
    }

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 3130536799704124363L;
  }
}