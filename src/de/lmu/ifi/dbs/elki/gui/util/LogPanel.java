package de.lmu.ifi.dbs.elki.gui.util;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.Progress;
import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;

/**
 * Panel that contains a text logging pane ({@link LogPane}) and progress bars.
 * 
 * @author Erich Schubert
 */
public class LogPanel extends JPanel {
  /**
   * Serial
   */
  private static final long serialVersionUID = 1L;

  /**
   * The actual text logging pane
   */
  protected LogPane logpane = new LogPane();

  /**
   * Current progress bars
   */
  protected HashMap<Progress, JProgressBar> pbarmap = new HashMap<Progress, JProgressBar>();

  /**
   * Constructor.
   */
  public LogPanel() {
    super();
    super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    JScrollPane outputPane = new JScrollPane(logpane);
    outputPane.setPreferredSize(new Dimension(200, 200));
    super.add(outputPane);
  }

  /**
   * Clear the current contents.
   */
  public void clear() {
    logpane.clear();
    for(Entry<Progress, JProgressBar> ent : pbarmap.entrySet()) {
      super.remove(ent.getValue());
      pbarmap.remove(ent.getKey());
    }
  }

  /**
   * Publish a logging record.
   * 
   * @param record Log record to publish
   */
  public void publish(final LogRecord record) {
    if(record instanceof ProgressLogRecord) {
      ProgressLogRecord preg = (ProgressLogRecord) record;
      Progress prog = preg.getProgress();
      JProgressBar pbar = pbarmap.get(prog);
      if(pbar != null) {
        if(prog instanceof FiniteProgress) {
          pbar.setValue(((FiniteProgress) prog).getProcessed());
          pbar.setString(((FiniteProgress) prog).toString());
        }
        else if(prog instanceof IndefiniteProgress) {
          pbar.setValue(((IndefiniteProgress) prog).getProcessed());
          pbar.setString(((IndefiniteProgress) prog).toString());
        }
        else {
          throw new RuntimeException("Unsupported progress record");
        }
      }
      else {
        if(prog instanceof FiniteProgress) {
          pbar = new JProgressBar(0, ((FiniteProgress) prog).getTotal());
          pbar.setValue(((FiniteProgress) prog).getProcessed());
          pbar.setString(((FiniteProgress) prog).toString());
          pbar.setStringPainted(true);
        }
        else if(prog instanceof IndefiniteProgress) {
          pbar = new JProgressBar();
          pbar.setIndeterminate(true);
          pbar.setValue(((IndefiniteProgress) prog).getProcessed());
          pbar.setString(((IndefiniteProgress) prog).toString());
          pbar.setStringPainted(true);
        }
        else {
          throw new RuntimeException("Unsupported progress record");
        }
        pbarmap.put(prog, pbar);
        final JProgressBar pbar2 = pbar;
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            addProgressBar(pbar2);
          }
        });
      }
      if(prog.isComplete() || prog instanceof StepProgress) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            try {
              logpane.publish(record);
            }
            catch(Exception e) {
              throw new RuntimeException("Error writing a log-like message.", e);
            }
          }
        });
      }
      if(prog.isComplete()) {
        pbarmap.remove(prog);
        final JProgressBar pbar2 = pbar;
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            removeProgressBar(pbar2);
          }
        });
      }
    }
    else {
      try {
        logpane.publish(record);
      }
      catch(Exception e) {
        throw new RuntimeException("Error writing a log-like message.", e);
      }
    }
  }

  /**
   * Print a message as if it were logged, without going through the full
   * logger.
   * 
   * @param message Message text
   * @param level Message level
   */
  public void publish(String message, Level level) {
    try {
      publish(new LogRecord(level, message));
    }
    catch(Exception e) {
      throw new RuntimeException("Error writing a log-like message.", e);
    }
  }

  /**
   * Become the default logger.
   */
  public void becomeDefaultLogger() {
    LoggingConfiguration.replaceDefaultHandler(new LogPanelHandler());
  }

  /**
   * Add a new progress bar.
   * 
   * @param pbar
   */
  protected void addProgressBar(final JProgressBar pbar) {
    super.add(pbar);
    super.revalidate();
  }

  /**
   * Remove a new progress bar.
   * 
   * @param pbar
   */
  protected void removeProgressBar(final JProgressBar pbar) {
    super.remove(pbar);
    super.revalidate();
  }

  /**
   * Internal {@link java.util.logging.Handler}
   * 
   * @author Erich Schubert
   */
  private class LogPanelHandler extends Handler {
    /**
     * Constructor.
     */
    protected LogPanelHandler() {
      super();
    }

    @Override
    public void close() throws SecurityException {
      // do nothing
    }

    @Override
    public void flush() {
      // do nothing
    }

    @Override
    public void publish(final LogRecord record) {
      try {
        LogPanel.this.publish(record);
      }
      catch(Exception e) {
        reportError("Error printing output log message.", e, ErrorManager.GENERIC_FAILURE);
      }
    }
  }
}