/*
 * Copyright (c) 2023, kumars2
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package M3M_SLM_UI;

import com.google.gson.Gson;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author kumars2
 */
public class SLM_controls extends javax.swing.JPanel {
    private M3M_SLM_hostframe parent_;
    double default_beamspacing_h = 4;
    double default_beamspacing_v = 3;
    public int n_beams_x = 2;
    public int n_beams_y = 3;
    Circle_To_Draw beams[][] = new Circle_To_Draw[n_beams_x][n_beams_y];
    int[] active_beam = {0,0};
    boolean initialised_ = false;
    private Color[] colour_list = {Color.red, Color.orange, Color.yellow, Color.green, Color.blue, Color.magenta, Color.cyan, Color.gray};
    int n_colours = 8;
    Gson gson = new Gson();
    JFileChooser fileChooser_load = new JFileChooser();
    JFileChooser fileChooser_save = new JFileChooser();
    FileFilter txtFilter = new FileTypeFilter(".txt", "Text files");
    boolean ignore_selection_activity = false;
    
    /**
     * Creates new form SLM_controls
     */
    public SLM_controls() {
        initComponents();
        fileChooser_load.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser_load.addChoosableFileFilter(txtFilter);
        fileChooser_save.setCurrentDirectory(new File(System.getProperty("user.home")));        
        fileChooser_save.addChoosableFileFilter(txtFilter);
    }
    
    public void set_parent(Object parentframe){
        parent_ = (M3M_SLM_hostframe) parentframe;
        initialised_ = true;
        setup_for_display();
    }    
    
    void setup_for_display(){
        generate_square_beam_array();
    }
    
    void sanitise(JTextField source_field){
        String input = source_field.getText();
        source_field.setText(parent_.utils.strip_non_numeric(input));
    }
       
    void update_beam_info(){
        parent_.update_beams(beams);
    }
    
    void update_beam_details(String beam_name,double new_xpos, double new_ypos){
        for(int x=0;x<n_beams_x;x++){
            for(int y=0;y<n_beams_y;y++){
                if(beams[x][y].get_name()==beam_name){
                    beams[x][y].set_xctr_mm(new_xpos);
                    beams[x][y].set_yctr_mm(new_ypos);
                }
            }
        }
        update_beam_info();
    }
    
    void generate_square_beam_array(){
        beams = new Circle_To_Draw[n_beams_x][n_beams_y];
        for(int x=0;x<n_beams_x;x++){
            for(int y=0;y<n_beams_y;y++){
                String name = "Beam_"+Integer.toString(x)+"_"+Integer.toString(y);
                //Generate new beam from scratch
                double range_x = ((double)n_beams_x-1)*default_beamspacing_h;
                double range_y = ((double)n_beams_y-1)*default_beamspacing_v;
                double x_spacing = 0;
                double y_spacing = 0;
                if(range_x != 0){
                    x_spacing = range_x/(n_beams_x-1);
                }
                if(range_y != 0){
                    y_spacing = range_y/(n_beams_y-1);
                }
                //In case we have a persistent offset
                double offset_x = 0;
                double offset_y = 0;
                beams[x][y] = new Circle_To_Draw(name,true, 0.5,(x*x_spacing)-(range_x/2)-offset_x,(y*y_spacing)-(range_y/2)-offset_y,colour_list[(y+(x*n_beams_y))%n_colours],false);
            }
        }
        update_dropdown_from_beams();
        if(initialised_){
            update_beam_info();
        }
    }
    
    void update_dropdown_from_beams(){
        ignore_selection_activity = true;
        beam_selected.removeAllItems();
        int to_highlight = 0;
        double sel_x_pos = beams[0][0].get_xctr_mm();
        double sel_y_pos = beams[0][0].get_yctr_mm();
        boolean any_highlighted = false;
        for(int x=0;x<n_beams_x;x++){
            for(int y=0;y<n_beams_y;y++){
                beam_selected.addItem(beams[x][y].get_name());
                if(beams[x][y].get_highlighted()){
                    to_highlight = ((x*n_beams_y)+y);
                    sel_x_pos = beams[x][y].get_xctr_mm();
                    sel_y_pos = beams[x][y].get_yctr_mm();
                    any_highlighted = true;
                }
            }
        }
        if(!any_highlighted){
            beams[0][0].set_highlighted(true);
        }
        beam_selected.setSelectedIndex(to_highlight);
        bs_xpos.setText(Double.toString(sel_x_pos));
        bs_ypos.setText(Double.toString(sel_y_pos));
        ignore_selection_activity = false;
    }
    
    void report_array_size(){
        System.out.println("============\nBeams in X: "+n_beams_x+"\nBeams in Y: "+n_beams_y);
    }
    
    int[] parse_beam_name(String name_in){
        int xval=0;
        int yval=0;
        String[] parts = name_in.split("_");
        xval = Integer.parseInt(parts[1]);
        yval = Integer.parseInt(parts[2]);
        int[] retarr = {xval,yval};
        //System.out.println(retarr);
        return(retarr);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TEST = new javax.swing.JButton();
        n_b_x_field = new javax.swing.JTextField();
        n_b_y_field = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        beam_selected = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        bs_xpos = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        bs_ypos = new javax.swing.JTextField();
        load_button = new javax.swing.JButton();
        save_button = new javax.swing.JButton();

        TEST.setText("TEST");
        TEST.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TESTActionPerformed(evt);
            }
        });

        n_b_x_field.setText("2");
        n_b_x_field.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                n_b_x_fieldActionPerformed(evt);
            }
        });

        n_b_y_field.setText("3");
        n_b_y_field.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                n_b_y_fieldActionPerformed(evt);
            }
        });

        jLabel1.setText("n_beams_x");

        jLabel2.setText("n_beams_y");

        beam_selected.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        beam_selected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                beam_selectedActionPerformed(evt);
            }
        });

        jLabel3.setText("Edit beam #:");

        jLabel4.setText("Xpos:");

        bs_xpos.setText("0");
        bs_xpos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bs_xposActionPerformed(evt);
            }
        });

        jLabel5.setText("Ypos:");

        bs_ypos.setText("0");
        bs_ypos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bs_yposActionPerformed(evt);
            }
        });

        load_button.setText("Load");
        load_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                load_buttonActionPerformed(evt);
            }
        });

        save_button.setText("Save");
        save_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(n_b_y_field)
                    .addComponent(beam_selected, 0, 117, Short.MAX_VALUE)
                    .addComponent(n_b_x_field))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(save_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(load_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(bs_ypos, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                    .addComponent(TEST, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(n_b_x_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(load_button)
                    .addComponent(TEST))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(n_b_y_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(save_button))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(beam_selected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void TESTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TESTActionPerformed
        //update_beam_info();
    }//GEN-LAST:event_TESTActionPerformed

    private void n_b_x_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_n_b_x_fieldActionPerformed
        sanitise(n_b_x_field);
        n_beams_x = Integer.parseInt(n_b_x_field.getText());
        generate_square_beam_array();
    }//GEN-LAST:event_n_b_x_fieldActionPerformed

    private void n_b_y_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_n_b_y_fieldActionPerformed
        sanitise(n_b_y_field);
        n_beams_y = Integer.parseInt(n_b_y_field.getText());
        generate_square_beam_array();
    }//GEN-LAST:event_n_b_y_fieldActionPerformed

    private void beam_selectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_beam_selectedActionPerformed
        if(initialised_ && ignore_selection_activity==false){
            String sel_beam = (String) beam_selected.getSelectedItem();
            for(int x=0;x<n_beams_x;x++){
                for(int y=0;y<n_beams_y;y++){
                    if(beams[x][y].get_name()==sel_beam){
                        bs_xpos.setText(Double.toString(beams[x][y].get_xctr_mm()));
                        bs_ypos.setText(Double.toString(beams[x][y].get_yctr_mm()));
                        beams[x][y].set_highlighted(true);
                    } else {
                        beams[x][y].set_highlighted(false);
                    }
                }
            }
            try{
                int[]beam_sel = parse_beam_name(sel_beam);
                active_beam[0] = beam_sel[0];
                active_beam[1] = beam_sel[1];
            } catch(Exception e){
                System.out.println("Error on selecting beam!");
            }
        }
    }//GEN-LAST:event_beam_selectedActionPerformed

    private void bs_xposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bs_xposActionPerformed
        sanitise(bs_xpos);
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_bs_xposActionPerformed

    private void bs_yposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bs_yposActionPerformed
        sanitise(bs_ypos);
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_bs_yposActionPerformed

    private void save_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_buttonActionPerformed
        String gsonified = gson.toJson(beams);
        int result = fileChooser_load.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser_load.getSelectedFile();
            if(selectedFile.isFile()){
                System.out.println("CAN'T OVERWRITE AN EXISTING FILE");
            } else {
                try {
                    selectedFile.createNewFile();
                    FileWriter Writer = new FileWriter(selectedFile);
                    Writer.write(gsonified);
                    //System.out.println(gsonified);
                    Writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_save_buttonActionPerformed

    private void load_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_load_buttonActionPerformed
        int result = fileChooser_load.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser_load.getSelectedFile();
            if(selectedFile.isFile()){
                String input = "";
                try{
                    Scanner Reader = new Scanner(selectedFile);
                    while (Reader.hasNextLine()){
                        String data = Reader.nextLine();
                        input += data;
                    }
                    Reader.close();
                    beams = gson.fromJson(input, Circle_To_Draw[][].class);
                    n_beams_x = beams.length;
                    if(n_beams_x>0){
                        n_beams_y = beams[0].length;
                    }
                    n_b_x_field.setText(Integer.toString(n_beams_x));
                    n_b_y_field.setText(Integer.toString(n_beams_y));
                    update_dropdown_from_beams();
                    update_beam_info();
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            } else {
                System.out.println("NOT AN EXISTING FILE?!");
            }
        }
    }//GEN-LAST:event_load_buttonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton TEST;
    private javax.swing.JComboBox<String> beam_selected;
    private javax.swing.JTextField bs_xpos;
    private javax.swing.JTextField bs_ypos;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JButton load_button;
    private javax.swing.JTextField n_b_x_field;
    private javax.swing.JTextField n_b_y_field;
    private javax.swing.JButton save_button;
    // End of variables declaration//GEN-END:variables
}
