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
package de.lmu.ifi.dbs.elki.gui.util;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.MutableProgress;
import de.lmu.ifi.dbs.elki.logging.progress.Progress;
import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;

/**
 * Panel that contains a text logging pane ({@link LogPane}) and progress bars.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @composed - - - LogPane
 * @assoc - - - LoggingConfiguration
 * @assoc - - - de.lmu.ifi.dbs.elki.logging.ELKILogRecord
 * @assoc - - - ProgressLogRecord
 * @assoc - - - LogPanelHandler
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
  protected HashMap<Progress, JProgressBar> pbarmap = new HashMap<>();

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
   * Become the default logger.
   */
  public void becomeDefaultLogger() {
    LoggingConfiguration.replaceDefaultHandler(new LogPanelHandler());
  }

  /**
   * Publish a logging record.
   * 
   * @param record Log record to publish
   */
  protected void publish(final LogRecord record) {
    if(record instanceof ProgressLogRecord) {
      ProgressLogRecord preg = (ProgressLogRecord) record;
      Progress prog = preg.getProgress();
      JProgressBar pbar = getOrCreateProgressBar(prog);
      updateProgressBar(prog, pbar);
      if(prog.isComplete()) {
        removeProgressBar(prog, pbar);
      }
      if(prog.isComplete() || prog instanceof StepProgress) {
        publishTextRecord(record);
      }
    }
    else {
      publishTextRecord(record);
    }
  }

  /**
   * Publish a text record to the pane
   * 
   * @param record Record to publish
   */
  private void publishTextRecord(final LogRecord record) {
    try {
      logpane.publish(record);
    }
    catch(Exception e) {
      throw new RuntimeException("Error writing a log-like message.", e);
    }
  }

  /**
   * Get an existing or create a new progress bar.
   * 
   * @param prog Progress
   * @return Associated progress bar.
   */
  private JProgressBar getOrCreateProgressBar(Progress prog) {
    JProgressBar pbar = pbarmap.get(prog);
    // Add a new progress bar.
    if(pbar == null) {
      synchronized(pbarmap) {
        if(prog instanceof FiniteProgress) {
          pbar = new JProgressBar(0, ((FiniteProgress) prog).getTotal());
          pbar.setStringPainted(true);
        }
        else if(prog instanceof IndefiniteProgress) {
          pbar = new JProgressBar();
          pbar.setIndeterminate(true);
          pbar.setStringPainted(true);
        }
        else if(prog instanceof MutableProgress) {
          pbar = new JProgressBar(0, ((MutableProgress) prog).getTotal());
          pbar.setStringPainted(true);
        }
        else {
          throw new RuntimeException("Unsupported progress record");
        }
        pbarmap.put(prog, pbar);
        final JProgressBar pbar2 = pbar; // Make final
        SwingUtilities.invokeLater(() -> addProgressBar(pbar2));
      }
    }
    return pbar;
  }

  /**
   * Update a progress bar
   * 
   * @param prog Progress
   * @param pbar Associated progress bar
   */
  private void updateProgressBar(Progress prog, JProgressBar pbar) {
    if(prog instanceof FiniteProgress) {
      pbar.setValue(((FiniteProgress) prog).getProcessed());
      pbar.setString(((FiniteProgress) prog).toString());
    }
    else if(prog instanceof IndefiniteProgress) {
      pbar.setValue(((IndefiniteProgress) prog).getProcessed());
      pbar.setString(((IndefiniteProgress) prog).toString());
    }
    else if(prog instanceof MutableProgress) {
      pbar.setValue(((MutableProgress) prog).getProcessed());
      pbar.setMaximum(((MutableProgress) prog).getProcessed());
      pbar.setString(((MutableProgress) prog).toString());
    }
    else {
      throw new RuntimeException("Unsupported progress record");
    }
  }

  /**
   * Remove a progress bar
   * 
   * @param prog Progress
   * @param pbar Associated progress bar
   */
  private void removeProgressBar(Progress prog, JProgressBar pbar) {
    synchronized(pbarmap) {
      pbarmap.remove(prog);
      SwingUtilities.invokeLater(() -> removeProgressBar(pbar));
    }
  }

  /**
   * Clear the current contents.
   */
  public void clear() {
    logpane.clear();
    synchronized(pbarmap) {
      for(Entry<Progress, JProgressBar> ent : pbarmap.entrySet()) {
        super.remove(ent.getValue());
        pbarmap.remove(ent.getKey());
      }
    }
  }

  /**
   * Add a new progress bar.
   * 
   * Protected, so this can be called via invokeLater
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
   * Protected, so this can be called via invokeLater
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
      LogPanel.this.publish(record);
    }
  }
}
