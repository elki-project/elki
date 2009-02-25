/**
 * 
 */
package experimentalcode.simon.gui.visu.save;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.batik.transcoder.TranscoderException;

import experimentalcode.erich.visualization.SVGPlot;

/**
 * A "generic" save dialog to save a SVG to disk. Supported formats:
 * <ul>
 * <li>JPEG (broken) (with width and height options)</li>
 * <li>PNG (with width and height options)</li>
 * <li>PDF</li>
 * <li>PS</li>
 * <li>EPS</li>
 * <li>SVG</li>
 * </ul>
 * 
 * @author simon
 * 
 */
public class SVGSaveDialog {

	/** The default title. "Save as ...". */
	public final static String DEFAULT_TITLE = "Save as ...";

	/** The file chooser. */
	private JFileChooser fc;

	/** The plot to be exported. */
	private SVGPlot svgPlot;
	/** The width of the exported image (if exported to JPEG or PNG). Default is 1024. */
	private int width = 1024;
	/** The height of the exported image (if exported to JPEG or PNG). Default is 768. */
	private int height = 768;

	/** The panel for the options (JPEG and PNG options) */
	private SaveOptionsPanel optionsPanel;

	// private JCheckBox cbAutoWidth;
	// private JCheckBox cbAutoHeight;

	/**
	 * A constructor to specify width and height hints for export to JPEG/PNG.
	 * 
	 * @param The plot to be exported.
	 * @param width The width of the exported image (when export to JPEG/PNG).
	 * @param height The height of the exported image (when export to JPEG/PNG).
	 * @see {@link SVGSaveDialog(SVGPlot)}
	 * */
	public SVGSaveDialog(SVGPlot plot, int width, int height) {
		this.width = width;
		this.height = height;
		initUI(plot);
	}

	/**
	 * A private constructor to specify width and height hints for export to JPEG/PNG. 
	 * Default width is 1024, default height is 768 
	 * (See {@link #width} and {@link #height}).
	 * 
	 * @param The plot to be exported.
	 * @see {@link SVGSaveDialog(SVGPlot, int, int)}
	 * */
//	private SVGSaveDialog(SVGPlot plot) {
//		super();
//		initUI(plot);
//	}

	/** Creates the components and handles the saving logic. */
	private void initUI(SVGPlot plot) {
		fc = new JFileChooser();
		fc.setDialogTitle(DEFAULT_TITLE);
		//fc.setFileFilter(new ImageFilter());
		optionsPanel = new SaveOptionsPanel(fc, width, height);
		fc.setAccessory(optionsPanel);

		int ret = fc.showSaveDialog(null);
		fc.setDialogTitle("Saving... Please wait.");
		if (ret == JFileChooser.APPROVE_OPTION) {
			String ext = optionsPanel.getImageFormat();
			try {
				File file = fc.getSelectedFile();
				if (ext.equals("jpeg") || ext.equals("jpg")) {
					plot.saveAsJPEG(file, width, height);
					//TODO: quality
				} else if (ext.equals("png")) {
					plot.saveAsPNG(file, width, height);
				} else if (ext.equals("ps")) {
					plot.saveAsPS(file);
				} else if (ext.equals("eps")) {
					plot.saveAsEPS(file);
				} else if (ext.equals("pdf")) {
					plot.saveAsPDF(file);
				} else if (ext.equals("svg")) {
					plot.saveAsSVG(file);
				} else {
					showError(fc, "Unsupported format: " + ext);
				}
			} catch (IOException ioe) {
				// TODO Auto-generated catch block
				ioe.printStackTrace();
				showError(fc, "Error: " + ioe);
			} catch (TranscoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showError(fc, "Error: " + e);
			} catch (TransformerFactoryConfigurationError tfce) {
				// TODO Auto-generated catch block
				tfce.printStackTrace();
				showError(fc, "Error: " + tfce);
			} catch (TransformerException te) {
				// TODO Auto-generated catch block
				te.printStackTrace();
				showError(fc, "Error: " + te);
			}
		} else if (ret == JFileChooser.ERROR_OPTION) {
			showError(fc, "Unknown Error.");
		} else if (ret == JFileChooser.CANCEL_OPTION) {

		}	
	}

	

	/**
	 * Helper method to show call {@link JOptionPane#showMessageDialog(java.awt.Component, Object)}.
	 * 
	 * @param parent The parent component for the popup.
	 * @param msg The message to be displayed.
	 * */
	private void showError(Component parent, String msg) {
		JOptionPane.showMessageDialog(parent, msg);
	}

	

}
