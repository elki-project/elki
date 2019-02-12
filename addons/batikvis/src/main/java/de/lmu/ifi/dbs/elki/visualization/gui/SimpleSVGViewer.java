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
package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A minimalistic SVG viewer with export dialog.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class SimpleSVGViewer extends JFrame {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * The main canvas.
   */
  private JSVGSynchronizedCanvas svgCanvas;

  /**
   * Constructor.
   * 
   * @throws HeadlessException
   */
  public SimpleSVGViewer() throws HeadlessException {
    super();
    // Prefer system look&feel
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      // ignore
    }

    // close handler
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    // Maximize.
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(dim.width - 50, dim.height - 50);

    // setup buttons
    JMenuItem exportItem = new JMenuItem("Export");
    exportItem.setMnemonic(KeyEvent.VK_E);
    exportItem.addActionListener((e) -> saveCurrentPlot());

    JMenuItem quitItem = new JMenuItem("Quit");
    quitItem.setMnemonic(KeyEvent.VK_Q);
    quitItem.addActionListener((e) -> close());

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

    // key commands
    KeyStroke ctrle = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrls = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlq = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlw = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
    panel.registerKeyboardAction((e) -> saveCurrentPlot(), ctrle, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> saveCurrentPlot(), ctrls, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> close(), ctrlq, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> close(), ctrlw, JComponent.WHEN_IN_FOCUSED_WINDOW);

    JMenuBar menubar = new JMenuBar();
    menubar.add(exportItem);
    menubar.add(quitItem);

    panel.add("North", menubar);

    svgCanvas = new JSVGSynchronizedCanvas();
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    setExtendedState(JFrame.MAXIMIZED_BOTH);
    this.setVisible(true);
  }

  /**
   * Close the visualizer window.
   */
  public void close() {
    this.setVisible(false);
    this.dispose();
  }

  /**
   * Save/export the current plot.
   */
  public void saveCurrentPlot() {
    // TODO: exclude "do not export" layers!
    final SVGPlot currentPlot = svgCanvas.getPlot();
    if(currentPlot != null) {
      SVGSaveDialog.showSaveDialog(currentPlot, 512, 512);
    }
    else {
      LoggingUtil.warning("saveCurrentPlot() called without a visible plot!");
    }
  }

  /**
   * Set the plot to show
   * 
   * @param plot Plot
   */
  public void setPlot(SVGPlot plot) {
    svgCanvas.setPlot(plot);
  }
}