package experimentalcode.erich.gearth;

import java.util.Stack;

/**
 * Class to produce JSON output.
 * 
 * @author Erich Schubert
 */
public class JSONBuffer {
  /**
   * The actual buffer we serialize to
   */
  StringBuffer buffer;

  /**
   * Operations on the stack.
   * 
   * @apiviz.exclude
   */
  private enum ops {
    HASH, ARRAY
  }

  /**
   * Operations stack for detecting errors
   */
  private Stack<ops> stack = new Stack<ops>();

  /**
   * Constructor.
   * 
   * @param buffer Buffer to serialize to
   */
  public JSONBuffer(StringBuffer buffer) {
    this.buffer = buffer;
  }

  /**
   * String escaping for JSON
   * 
   * @param orig Original string
   * @return JSON safe string
   */
  public static String jsonEscapeString(String orig) {
    return orig.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /**
   * Append a string in quotes
   * 
   * @param cont Contents
   * @return Buffer for chaining
   */
  public JSONBuffer appendString(Object cont) {
    if(stack.empty() || stack.peek() != ops.ARRAY) {
      throw new JSONException("Appending string outside of array context.");
    }
    addQuotedString(cont);
    addSeparator();
    return this;
  }

  /**
   * Append double
   * 
   * @param cont Contents
   * @return Buffer for chaining
   */
  public JSONBuffer append(double cont) {
    if(stack.empty() || stack.peek() != ops.ARRAY) {
      throw new JSONException("Appending double outside of array context.");
    }
    buffer.append(Double.toString(cont));
    addSeparator();
    return this;
  }

  /**
   * Append integer
   * 
   * @param cont Contents
   * @return Buffer for chaining
   */
  public JSONBuffer append(int cont) {
    if(stack.empty() || stack.peek() != ops.ARRAY) {
      throw new JSONException("Appending double outside of array context.");
    }
    buffer.append(Integer.toString(cont));
    addSeparator();
    return this;
  }

  /**
   * Append double array
   * 
   * @param cont Contents
   * @return Buffer for chaining
   */
  public JSONBuffer append(double[] cont) {
    startArray();
    for(int i = 0; i < cont.length; i++) {
      if(i > 0) {
        buffer.append(",");
      }
      buffer.append(Double.toString(cont[i]));
    }
    closeArray();
    return this;
  }

  /**
   * Append integer array
   * 
   * @param cont Contents
   * @return Buffer for chaining
   */
  public JSONBuffer append(int[] cont) {
    startArray();
    for(int i = 0; i < cont.length; i++) {
      if(i > 0) {
        buffer.append(",");
      }
      buffer.append(Integer.toString(cont[i]));
    }
    closeArray();
    return this;
  }

  /**
   * Add a string in quotes.
   * 
   * @param cont Object to put as string
   */
  private void addQuotedString(Object cont) {
    final String str;
    if(cont instanceof String) {
      str = (String) cont;
    }
    else if(cont == null) {
      str = "null";
    }
    else {
      str = cont.toString();
    }
    buffer.append("\"").append(jsonEscapeString(str)).append("\"");
  }

  /**
   * Append a key-value pair as string.
   * 
   * @param key Key to append
   * @param val Value to append
   * @return Buffer for chaining
   */
  public JSONBuffer appendKeyValue(Object key, Object val) {
    if(stack.empty() || stack.peek() != ops.HASH) {
      throw new JSONException("Appending key-value outside of hash context.");
    }
    addQuotedString(key);
    buffer.append(":");
    if(val instanceof Double) {
      buffer.append(val.toString());
    }
    else if(val instanceof Integer) {
      buffer.append(val.toString());
    }
    else {
      addQuotedString(val);
    }
    buffer.append(",");
    return this;
  }

  /**
   * Append a key an start a new hash
   * 
   * @param key Key to append
   * @return Buffer for chaining
   */
  public JSONBuffer appendKeyHash(Object key) {
    if(stack.empty() || stack.peek() != ops.HASH) {
      throw new JSONException("Appending key-value outside of hash context.");
    }
    addQuotedString(key);
    buffer.append(":");
    buffer.append("{");
    stack.push(ops.HASH);
    return this;
  }

  /**
   * Append a key an start a new array
   * 
   * @param key Key to append
   * @return Buffer for chaining
   */
  public JSONBuffer appendKeyArray(Object key) {
    if(stack.empty() || stack.peek() != ops.HASH) {
      throw new JSONException("Appending key-value outside of hash context.");
    }
    addQuotedString(key);
    buffer.append(":");
    buffer.append("[");
    stack.push(ops.ARRAY);
    return this;
  }

  /**
   * Start an array context. Must only be called on an empty or array context.
   * 
   * @return Buffer for chaining
   */
  public JSONBuffer startArray() {
    if(!stack.empty() && stack.peek() != ops.ARRAY) {
      throw new JSONException("startArray() is only allowed in an empty context.");
    }
    buffer.append("[");
    stack.push(ops.ARRAY);
    return this;
  }

  /**
   * Start an hash context. Must only be called on an empty or array context.
   * 
   * @return Buffer for chaining
   */
  public JSONBuffer startHash() {
    if(!stack.empty() && stack.peek() != ops.ARRAY) {
      throw new JSONException("startHash() is only allowed in an empty context.");
    }
    buffer.append("{");
    stack.push(ops.HASH);
    return this;
  }

  /**
   * Close an array context.
   * 
   * @return Buffer for chaining
   */
  public JSONBuffer closeArray() {
    if(stack.empty() || stack.peek() != ops.ARRAY) {
      throw new JSONException("Not in array context when closing.");
    }
    removeSeparator();
    buffer.append("]");
    stack.pop();
    addSeparator();
    return this;
  }

  /**
   * Close an hash context.
   * 
   * @return Buffer for chaining
   */
  public JSONBuffer closeHash() {
    if(stack.empty() || stack.peek() != ops.HASH) {
      throw new JSONException("Not in array context when closing.");
    }
    removeSeparator();
    buffer.append("}");
    stack.pop();
    addSeparator();
    return this;
  }

  /**
   * Remove a separator character if present
   */
  private void removeSeparator() {
    if(buffer.charAt(buffer.length() - 1) == ',') {
      buffer.deleteCharAt(buffer.length() - 1);
    }
  }

  /**
   * Add a separator
   */
  private void addSeparator() {
    if(stack.empty()) {
      return;
    }
    switch(stack.peek()){
    case HASH:
    case ARRAY:
      buffer.append(",");
      break;
    default:
      break;
    }
  }

  /**
   * Append a newline, for human readability
   * 
   * @return Buffer for chaining
   */
  public JSONBuffer appendNewline() {
    buffer.append("\n");
    return this;
  }

  /**
   * Class to represent JSON encoding exceptions.
   * 
   * @author Erich Schubert
   */
  public static class JSONException extends RuntimeException {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public JSONException() {
      super();
    }

    /**
     * Constructor.
     * 
     * @param message Error message
     * @param cause Cause
     */
    public JSONException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Constructor.
     * 
     * @param message Error message
     */
    public JSONException(String message) {
      super(message);
    }

    /**
     * Constructor.
     * 
     * @param cause Cause
     */
    public JSONException(Throwable cause) {
      super(cause);
    }
  }
}