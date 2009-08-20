package experimentalcode.erich.minigui;

import java.awt.Color;
import java.util.logging.Level;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import experimentalcode.erich.minigui.GUILogHandler.GUILogReceiver;

public class LogPane extends JTextPane implements GUILogReceiver {
  /**
   * Serialization version number
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Base (default) style
   */
  protected Style baseStyle;
  
  /**
   * Regular message style
   */
  protected Style msgStyle;
  
  /**
   * Error message style
   */
  protected Style errStyle;

  /**
   * Constructor
   */
  public LogPane() {
    super();
    // setup styles
    baseStyle = getStyledDocument().addStyle(null, null);
    msgStyle = getStyledDocument().addStyle("msg", baseStyle);
    errStyle = getStyledDocument().addStyle("err", baseStyle);
    errStyle.addAttribute(StyleConstants.Foreground, Color.RED);
  }

  @Override
  public void publishLogMessage(String record, Level level) {
    final Style style;
    if (level.intValue() >= Level.WARNING.intValue()) {
      style = errStyle;
    } else {
      style = msgStyle;
    }
    try {
      getStyledDocument().insertString(getStyledDocument().getLength(), record, style);
    }
    catch(BadLocationException e2) {
      // Can't rely on logger here, might recurse
      System.err.println("Error in managing output log document: " + e2.getMessage());
    }
  }
  
  /**
   * Clear the current contents.
   */
  public void clear() {
    setText("");
  }
}
