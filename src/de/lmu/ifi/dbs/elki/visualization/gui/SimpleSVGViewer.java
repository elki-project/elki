package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A minimalistic SVG viewer with export dialog.
 * 
 * @author Erich Schubert
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
    exportItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        saveCurrentPlot();
      }
    });

    JMenuItem quitItem = new JMenuItem("Quit");
    quitItem.setMnemonic(KeyEvent.VK_Q);
    quitItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        close();
      }
    });

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

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