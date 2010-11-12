package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import xmlSplitter.Splitter;

/**
 * Class to display the WikipediaNER GUI
 * 
 * @date 13-11-2010
 * @author Jessica Lundberg
 * 
 */
public class MainFrame {
    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event-dispatching thread.
     */
    private void createAndShowGUI() {
	// Create and set up the window.
	JFrame frame = new JFrame("TopLevelDemo");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	setUpMenu(frame);
	// Display the window.
	frame.pack();
	frame.setVisible(true);

    }

    /**
     * Set up JFrame menu
     * 
     * @param jframe Top-level Container being used
     *           
     */
    public void setUpMenu(JFrame jframe) {
	// Create the menu bar. Make it have an awesome torquoise background
	JMenuBar menuBar = new JMenuBar();
	menuBar.setOpaque(true);
	menuBar.setBackground(new Color(118, 230, 200));
	menuBar.setPreferredSize(new Dimension(200, 20));

	// create first menu, run
	JMenu menu = new JMenu("Run");
	menu.setBackground(new Color(118, 230, 200));
	menuBar.add(menu);

	// create menu items with accompanying action..
	JMenuItem runSplitter = new JMenuItem("Run Splitter", KeyEvent.VK_T);
	runSplitter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
		ActionEvent.ALT_MASK));
	runSplitter.getAccessibleContext().setAccessibleDescription(
		"Runs the Splitter Program");
	runSplitter.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		Splitter splitter = new Splitter();
		splitter.split();
	    }
	});
	menu.add(runSplitter);

	// Create a yellow label to put in the content pane.
	JLabel yellowLabel = new JLabel();
	yellowLabel.setOpaque(true);
	yellowLabel.setBackground(new Color(118, 230, 200));
	yellowLabel.setPreferredSize(new Dimension(200, 180));

	// Set the menu bar and add the label to the content pane.
	jframe.setJMenuBar(menuBar);
	jframe.getContentPane().add(yellowLabel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
	// Schedule a job for the event-dispatching thread:
	// creating and showing this application's GUI.
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		MainFrame mainFrame = new MainFrame();
		mainFrame.createAndShowGUI();
	    }
	});
    }
}