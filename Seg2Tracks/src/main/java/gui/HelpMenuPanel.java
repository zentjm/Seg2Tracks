package gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;

public class HelpMenuPanel extends JFrame implements ActionListener{

	//Fill out help general info
	JTextPane buttonGeneralInfo = new JTextPane();
	JButton buttonQuit = new JButton("Guided Calibration");
	
	//Create Panel
	GridBagConstraints constraints;
	JPanel panel = new JPanel(new GridBagLayout());
	

	
	public HelpMenuPanel() {
		createView();
		}

	public void createView() {
		
		//Sets JFrame Size
		setMinimumSize(new Dimension(200,200));
				
		//Create a new panel with GridBagLayout manager
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
	      
        //ROW 0
      	constraints.gridy = 0;
        
      	//Sigma Label
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.gridx = 0;
        panel.add(new JLabel("Help Menu"), constraints);
        
        
    	//Noise Label
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.gridx = 1;
        
        //ROW 1
      	constraints.gridy = 1;
        
        
    	//Finalize JFrame/JPanel
		add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
	}
	
	
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
