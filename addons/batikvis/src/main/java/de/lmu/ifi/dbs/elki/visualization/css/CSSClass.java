/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.css;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class representing a single CSS class.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @navassoc - - - InvalidCSS
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
    this.owner = new WeakReference<>(owner);
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
      this.statements = new ArrayList<>();
    }
  }
  
  /**
   * Simplified constructor, empty statements list.
   * 
   * @param owner Class owner.
   * @param name Class name.
   */
  public CSSClass(Object owner, String name) {
    this(owner, name, (Collection<Pair<String,String>>) null);
  }
  
  /**
   * Cloning constructor
   * 
   * @param owner Class owner.
   * @param name Class name.
   * @param other Class to clone
   */
  public CSSClass(Object owner, String name, CSSClass other) {
    this(owner, name, new ArrayList<>(other.statements));
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
   * @param statements Statements to check
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
   * Get read-only collection access to all statements.
   * 
   * @return Collection
   */
  public Collection<Pair<String, String>> getStatements() {
    return Collections.unmodifiableCollection(statements);
  }
  
  /**
   * Set a CSS statement.
   * 
   * @param key Statement key.
   * @param value Value or null (to unset)
   */
  public void setStatement(String key, String value) {
    if (value != null && !checkCSSStatement(key, value)) {
      throw new InvalidCSS("Invalid CSS statement.");
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
      statements.add(new Pair<>(key, value));
    }
  }

  /**
   * Set a CSS statement.
   * 
   * @param key Statement key.
   * @param value Value
   */
  public void setStatement(String key, int value) {
    setStatement(key, Integer.toString(value));
  }

  /**
   * Set a CSS statement.
   * 
   * @param key Statement key.
   * @param value Value
   */
  public void setStatement(String key, double value) {
    setStatement(key, Double.toString(value));
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
  public void appendCSSDefinition(StringBuilder buf) {
    buf.append("\n.");
    buf.append(name);
    buf.append('{');
    for (Pair<String, String> pair : statements) {
      buf.append(pair.getFirst());
      buf.append(':');
      buf.append(pair.getSecond());
      buf.append(";\n");
    }
    buf.append("}\n");
  }
  
  /**
   * Exception class thrown when encountering invalid CSS.
   */
  public static class InvalidCSS extends RuntimeException {
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

  /**
   * Render CSS class to inline formatting
   * 
   * @return string rendition of CSS for inline use
   */
  public String inlineCSS() {
    StringBuilder buf = new StringBuilder();
    for (Pair<String, String> pair : statements) {
      buf.append(pair.getFirst());
      buf.append(':');
      buf.append(pair.getSecond());
      buf.append(';');
    }
    return buf.toString();
  }
}