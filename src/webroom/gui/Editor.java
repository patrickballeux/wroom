/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.gui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import webroom.engine.MapEntry;
import webroom.engine.Message;
import webroom.engine.RoomFile;

/**
 *
 * @author patri
 */
public class Editor extends javax.swing.JDialog implements Message {

    private RoomFile mRoomFile;
    private DefaultTableModel modelMap;

    /**
     * Creates new form Editor
     */
    public Editor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setIconImage(new ImageIcon(Editor.class.getResource("logo.png")).getImage());
        modelMap = new DefaultTableModel();
        tableMap.setModel(modelMap);
        buildMap(15, 15);
        tableMap.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                bindEntry(tableMap.getSelectedRow(), tableMap.getSelectedColumn());
            }
        });
        lstTextures.setModel(new DefaultListModel<>());
    }

    private void buildMap(int width, int height) {
        MapEntry[][] entries = new MapEntry[width][height];
        String[] names = new String[width];
        for (int x = 0; x < width; x++) {
            names[x] = "" + x;
            for (int y = 0; y < height; y++) {
                entries[x][y] = new MapEntry();

            }
        }
        
        modelMap = new DefaultTableModel(entries, names){
            public boolean isCellEditable(int row, int col){
                return false;
            }
        };
        tableMap.setModel(modelMap);
        //tableMap.getTableHeader().setUI(null);
        tableMap.setRowHeight(20);
        for (int i = 0;i<tableMap.getColumnCount();i++){
            tableMap.setDefaultRenderer(tableMap.getColumnClass(i), new MapEntryRenderer());
        }
    }

    private void bindEntry(int row, int col) {
        if ( row != -1 && col != -1) {
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel) cboTextures.getModel();
            model.removeAllElements();
            model.addElement("None");
            for (int i = 0; i < lstTextures.getModel().getSize(); i++) {
                model.addElement(lstTextures.getModel().getElementAt(i));
            }
            MapEntry entry = (MapEntry) tableMap.getValueAt(row, col);
            if (entry.getTexture() == null) {
                cboTextures.setSelectedIndex(0);
            } else {
                cboTextures.setSelectedItem(entry.getTexture().toString());
            }
            if (entry.getBrowse() == null) {
                txtEntryBrowse.setText("");
            } else {
                txtEntryBrowse.setText(entry.getBrowse().toString());
            }
            if (entry.getDownload() == null) {
                txtEntryDownload.setText("");
            } else {
                txtEntryDownload.setText(entry.getDownload().toString());
            }
            if (entry.getEmbededText() == null) {
                txtEntryText.setText("");
            } else {
                txtEntryText.setText(entry.getText());
            }
            chkEntryDoor.setSelected(entry.isIsDoor());
            txtMapX.setText(row + "");
            txtMapY.setText(col + "");

        }

    }

    public void load(File roomFile) throws MalformedURLException, IOException {
        mRoomFile = new RoomFile(roomFile.toURI().toURL(), this);
    }

    public void load(RoomFile f) {
        mRoomFile = f;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabs = new javax.swing.JTabbedPane();
        panMap = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableMap = new javax.swing.JTable();
        panEntryDetails = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        cboTextures = new javax.swing.JComboBox<>();
        txtEntrySound = new javax.swing.JTextField();
        txtEntryDownload = new javax.swing.JTextField();
        txtEntryBrowse = new javax.swing.JTextField();
        txtEntryMedia = new javax.swing.JTextField();
        txtEntryTeleport = new javax.swing.JTextField();
        txtEntryText = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        txtEntryEmbeded = new javax.swing.JTextField();
        chkEntryDoor = new javax.swing.JCheckBox();
        txtMapX = new javax.swing.JTextField();
        txtMapY = new javax.swing.JTextField();
        panTextures = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstTextures = new javax.swing.JList<>();
        btnAddTexture = new javax.swing.JButton();
        btnRemoveTexture = new javax.swing.JButton();
        panLogs = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtLogs = new javax.swing.JTextArea();
        btnClearLogs = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("WRoom Editor");

        tableMap.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tableMap.setCellSelectionEnabled(true);
        tableMap.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                tableMapPropertyChange(evt);
            }
        });
        jScrollPane2.setViewportView(tableMap);

        panEntryDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Map Entry"));

        jLabel1.setText("X");

        jLabel2.setText("Y");

        jLabel3.setText("Texture");

        jLabel4.setText("Text");

        jLabel5.setText("Sound");

        jLabel6.setText("Download");

        jLabel7.setText("Browse to");

        jLabel8.setText("Teleport to");

        jLabel9.setText("Media");

        cboTextures.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None" }));
        cboTextures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboTexturesActionPerformed(evt);
            }
        });

        txtEntrySound.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEntrySoundActionPerformed(evt);
            }
        });

        txtEntryTeleport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEntryTeleportActionPerformed(evt);
            }
        });

        jLabel10.setText("Embeded");

        chkEntryDoor.setText("Is a door");

        txtMapX.setEditable(false);
        txtMapX.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtMapX.setText("0");

        txtMapY.setEditable(false);
        txtMapY.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtMapY.setText("0");

        javax.swing.GroupLayout panEntryDetailsLayout = new javax.swing.GroupLayout(panEntryDetails);
        panEntryDetails.setLayout(panEntryDetailsLayout);
        panEntryDetailsLayout.setHorizontalGroup(
            panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panEntryDetailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panEntryDetailsLayout.createSequentialGroup()
                        .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel9)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel10))
                        .addGap(33, 33, 33)
                        .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtEntryEmbeded)
                            .addComponent(txtEntryDownload)
                            .addComponent(txtEntrySound)
                            .addComponent(txtEntryText)
                            .addComponent(cboTextures, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtEntryBrowse)
                            .addComponent(txtEntryMedia)))
                    .addGroup(panEntryDetailsLayout.createSequentialGroup()
                        .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panEntryDetailsLayout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtMapX, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel8))
                        .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panEntryDetailsLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtMapY, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 134, Short.MAX_VALUE)
                                .addComponent(chkEntryDoor))
                            .addGroup(panEntryDetailsLayout.createSequentialGroup()
                                .addGap(7, 7, 7)
                                .addComponent(txtEntryTeleport)))))
                .addContainerGap())
        );
        panEntryDetailsLayout.setVerticalGroup(
            panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panEntryDetailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(chkEntryDoor)
                    .addComponent(txtMapX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtMapY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cboTextures, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtEntryText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtEntrySound, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtEntryDownload, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtEntryBrowse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtEntryTeleport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtEntryMedia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panEntryDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(txtEntryEmbeded, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panMapLayout = new javax.swing.GroupLayout(panMap);
        panMap.setLayout(panMapLayout);
        panMapLayout.setHorizontalGroup(
            panMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panMapLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(panEntryDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panMapLayout.setVerticalGroup(
            panMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panMapLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panEntryDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        tabs.addTab("Map", panMap);

        lstTextures.setBorder(javax.swing.BorderFactory.createTitledBorder("Textures"));
        lstTextures.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(lstTextures);

        btnAddTexture.setText("Add");
        btnAddTexture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddTextureActionPerformed(evt);
            }
        });

        btnRemoveTexture.setText("Remove");

        javax.swing.GroupLayout panTexturesLayout = new javax.swing.GroupLayout(panTextures);
        panTextures.setLayout(panTexturesLayout);
        panTexturesLayout.setHorizontalGroup(
            panTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panTexturesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                    .addGroup(panTexturesLayout.createSequentialGroup()
                        .addComponent(btnAddTexture)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRemoveTexture)))
                .addContainerGap())
        );
        panTexturesLayout.setVerticalGroup(
            panTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panTexturesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddTexture)
                    .addComponent(btnRemoveTexture))
                .addContainerGap(217, Short.MAX_VALUE))
        );

        tabs.addTab("Resources", panTextures);

        txtLogs.setEditable(false);
        txtLogs.setColumns(20);
        txtLogs.setRows(5);
        jScrollPane1.setViewportView(txtLogs);

        btnClearLogs.setText("Clear");
        btnClearLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearLogsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panLogsLayout = new javax.swing.GroupLayout(panLogs);
        panLogs.setLayout(panLogsLayout);
        panLogsLayout.setHorizontalGroup(
            panLogsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panLogsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panLogsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panLogsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnClearLogs)))
                .addContainerGap())
        );
        panLogsLayout.setVerticalGroup(
            panLogsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panLogsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnClearLogs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabs.addTab("Output", panLogs);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnClearLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearLogsActionPerformed
        txtLogs.setText("");
    }//GEN-LAST:event_btnClearLogsActionPerformed

    private void txtEntrySoundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEntrySoundActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEntrySoundActionPerformed

    private void txtEntryTeleportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEntryTeleportActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEntryTeleportActionPerformed

    private void tableMapPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_tableMapPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_tableMapPropertyChange

    private void btnAddTextureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddTextureActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".jpg");
            }

            @Override
            public String getDescription() {
                return "Images";
            }
        });
        chooser.showOpenDialog(this);
        
        if (chooser.getSelectedFiles().length != 0) {
            File[] files = chooser.getSelectedFiles();
            DefaultListModel<String> model = (DefaultListModel) lstTextures.getModel();
            for (File f : files) {
                try {
                    model.addElement(f.toURI().toURL().toString());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (chooser.getSelectedFile() != null){
            DefaultListModel<String> model = (DefaultListModel) lstTextures.getModel();
            try {
                model.addElement(chooser.getSelectedFile().toURI().toURL().toString());
            } catch (MalformedURLException ex) {
                Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_btnAddTextureActionPerformed

    private void cboTexturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboTexturesActionPerformed
        if (tableMap.getSelectedColumn() > 0 && tableMap.getSelectedRow() > 0) {
            MapEntry entry = (MapEntry) tableMap.getValueAt(tableMap.getSelectedRow(), tableMap.getSelectedColumn());
            if (cboTextures.getSelectedIndex() > 0) {
                try {
                    entry.setTexture(new URL(cboTextures.getSelectedItem().toString()));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                entry.setTexture(null);
            }
        }
        tableMap.updateUI();

    }//GEN-LAST:event_cboTexturesActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Editor dialog = new Editor(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddTexture;
    private javax.swing.JButton btnClearLogs;
    private javax.swing.JButton btnRemoveTexture;
    private javax.swing.JComboBox<String> cboTextures;
    private javax.swing.JCheckBox chkEntryDoor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JList<String> lstTextures;
    private javax.swing.JPanel panEntryDetails;
    private javax.swing.JPanel panLogs;
    private javax.swing.JPanel panMap;
    private javax.swing.JPanel panTextures;
    private javax.swing.JTable tableMap;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTextField txtEntryBrowse;
    private javax.swing.JTextField txtEntryDownload;
    private javax.swing.JTextField txtEntryEmbeded;
    private javax.swing.JTextField txtEntryMedia;
    private javax.swing.JTextField txtEntrySound;
    private javax.swing.JTextField txtEntryTeleport;
    private javax.swing.JTextField txtEntryText;
    private javax.swing.JTextArea txtLogs;
    private javax.swing.JTextField txtMapX;
    private javax.swing.JTextField txtMapY;
    // End of variables declaration//GEN-END:variables

    @Override
    public void OnAction(int x, int y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void OnTrigger(int x, int y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void OnNotification(int x, int y, String msg) {
        txtLogs.append(msg + "\n");
    }

    @Override
    public void OnError(int x, int y, String error) {
        txtLogs.append(error + "\n");
    }

    @Override
    public void onCloseView(JComponent comp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
