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
import java.util.Arrays;
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
    public int n_beams_x = 1;
    public int n_beams_y = 1;
    Circle_To_Draw[][] active_beams = new Circle_To_Draw[n_beams_x][n_beams_y];
    int max_stored_beams = 100;
    int current_stored_beam = 0;
    Circle_To_Draw[] stored_beams = new Circle_To_Draw[max_stored_beams];
    int[] active_beam = {0,0};
    boolean initialised_ = false;
    private Color[] colour_list = {Color.red, Color.orange, Color.yellow, Color.green, Color.blue, Color.magenta, Color.cyan, Color.gray};
    int n_colours = 8;
    Gson gson = new Gson();
    JFileChooser fileChooser_load = new JFileChooser();
    JFileChooser fileChooser_save = new JFileChooser();
    FileFilter txtFilter = new FileTypeFilter(".txt", "Text files");
    boolean ignore_selection_activity = false;
    boolean show_stored_beams = false;
    
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
        Circle_To_Draw[] truncated_stored_beams = Arrays.copyOfRange(stored_beams, 0, current_stored_beam);
        parent_.update_beams(active_beams,truncated_stored_beams,show_stored_beams);
    }
    
    void update_beam_details(String beam_name,double new_xpos, double new_ypos){
        for(int x=0;x<n_beams_x;x++){
            for(int y=0;y<n_beams_y;y++){
                if(active_beams[x][y].get_name()==beam_name){
                    active_beams[x][y].set_xctr_mm(new_xpos);
                    active_beams[x][y].set_yctr_mm(new_ypos);
                }
            }
        }
        update_beam_info();
    }
    
    void generate_square_beam_array(){
        active_beams = new Circle_To_Draw[n_beams_x][n_beams_y];
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
                active_beams[x][y] = new Circle_To_Draw(name,true, 0.5,(x*x_spacing)-(range_x/2)-offset_x,(y*y_spacing)-(range_y/2)-offset_y,colour_list[(y+(x*n_beams_y))%n_colours],false);
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
        double sel_x_pos = active_beams[0][0].get_xctr_mm();
        double sel_y_pos = active_beams[0][0].get_yctr_mm();
        boolean any_highlighted = false;
        for(int x=0;x<n_beams_x;x++){
            for(int y=0;y<n_beams_y;y++){
                beam_selected.addItem(active_beams[x][y].get_name());
                if(active_beams[x][y].get_highlighted()){
                    to_highlight = ((x*n_beams_y)+y);
                    sel_x_pos = active_beams[x][y].get_xctr_mm();
                    sel_y_pos = active_beams[x][y].get_yctr_mm();
                    any_highlighted = true;
                }
            }
        }
        if(!any_highlighted){
            active_beams[0][0].set_highlighted(true);
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

        add_to_list_button = new javax.swing.JButton();
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
        UP_BUTTON = new javax.swing.JButton();
        DOWN_BUTTON = new javax.swing.JButton();
        RIGHT_BUTTON = new javax.swing.JButton();
        LEFT_BUTTON = new javax.swing.JButton();
        h_nudge = new javax.swing.JTextField();
        v_nudge = new javax.swing.JTextField();
        generate_hologram_button = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(50, 50), new java.awt.Dimension(50, 50), new java.awt.Dimension(50, 50));
        show_stored = new javax.swing.JCheckBox();
        gen_square_button = new javax.swing.JButton();

        add_to_list_button.setText("Add to list");
        add_to_list_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_to_list_buttonActionPerformed(evt);
            }
        });

        n_b_x_field.setText("1");
        n_b_x_field.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                n_b_x_fieldActionPerformed(evt);
            }
        });

        n_b_y_field.setText("1");
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

        load_button.setText("Load pattern");
        load_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                load_buttonActionPerformed(evt);
            }
        });

        save_button.setText("Save pattern");
        save_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_buttonActionPerformed(evt);
            }
        });

        UP_BUTTON.setText("/\\");
            UP_BUTTON.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    UP_BUTTONActionPerformed(evt);
                }
            });

            DOWN_BUTTON.setText("\\/");
            DOWN_BUTTON.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    DOWN_BUTTONActionPerformed(evt);
                }
            });

            RIGHT_BUTTON.setText(">");
            RIGHT_BUTTON.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    RIGHT_BUTTONActionPerformed(evt);
                }
            });

            LEFT_BUTTON.setText("<");
            LEFT_BUTTON.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    LEFT_BUTTONActionPerformed(evt);
                }
            });

            h_nudge.setText("0.1");
            h_nudge.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    h_nudgeActionPerformed(evt);
                }
            });

            v_nudge.setText("0.1");
            v_nudge.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    v_nudgeActionPerformed(evt);
                }
            });

            generate_hologram_button.setText("Generate full hologram");

            jLabel6.setText("h nudge");

            jLabel7.setText("v nudge");

            show_stored.setText("Show stored beams");
            show_stored.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    show_storedActionPerformed(evt);
                }
            });

            gen_square_button.setText("Generate beam array");
            gen_square_button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    gen_square_buttonActionPerformed(evt);
                }
            });

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
            this.setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel6)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(h_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel7)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(v_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGap(126, 126, 126))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(226, 226, 226)
                            .addComponent(jLabel4)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                    .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel5)
                            .addGap(18, 18, 18)
                            .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(LEFT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(UP_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(DOWN_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(RIGHT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addContainerGap(12, Short.MAX_VALUE))))
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(jLabel2)
                                .addComponent(jLabel3))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(n_b_y_field)
                                .addComponent(beam_selected, 0, 117, Short.MAX_VALUE)
                                .addComponent(n_b_x_field))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(gen_square_button)
                            .addGap(43, 43, 43)
                            .addComponent(generate_hologram_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGap(12, 12, 12))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(16, 16, 16)
                            .addComponent(show_stored)
                            .addGap(43, 43, 43)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(save_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(add_to_list_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(load_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(19, 19, 19)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(n_b_x_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)
                        .addComponent(generate_hologram_button)
                        .addComponent(gen_square_button))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(n_b_y_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2))
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(beam_selected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)
                        .addComponent(jLabel4)
                        .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5)
                        .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(27, 27, 27)
                            .addComponent(UP_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(filler2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(RIGHT_BUTTON, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(LEFT_BUTTON, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(DOWN_BUTTON, javax.swing.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE)
                            .addContainerGap())
                        .addGroup(layout.createSequentialGroup()
                            .addGap(46, 46, 46)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(h_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel6))
                                .addComponent(add_to_list_button))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(v_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel7))
                                    .addGap(18, 18, 18)
                                    .addComponent(show_stored))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(load_button)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(save_button)))
                            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            );
        }// </editor-fold>//GEN-END:initComponents

    private void add_to_list_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_to_list_buttonActionPerformed
        if(current_stored_beam<max_stored_beams){
            stored_beams[current_stored_beam] = new Circle_To_Draw(active_beams[0][0]);
            current_stored_beam++;
            update_beam_info();
        } else {
            System.out.println("Too many beams to store!");
        }
    }//GEN-LAST:event_add_to_list_buttonActionPerformed

    private void n_b_x_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_n_b_x_fieldActionPerformed
        sanitise(n_b_x_field);
        n_beams_x = Integer.parseInt(n_b_x_field.getText());
    }//GEN-LAST:event_n_b_x_fieldActionPerformed

    private void n_b_y_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_n_b_y_fieldActionPerformed
        sanitise(n_b_y_field);
        n_beams_y = Integer.parseInt(n_b_y_field.getText());
    }//GEN-LAST:event_n_b_y_fieldActionPerformed

    private void beam_selectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_beam_selectedActionPerformed
        if(initialised_ && ignore_selection_activity==false){
            String sel_beam = (String) beam_selected.getSelectedItem();
            for(int x=0;x<n_beams_x;x++){
                for(int y=0;y<n_beams_y;y++){
                    if(active_beams[x][y].get_name()==sel_beam){
                        bs_xpos.setText(Double.toString(active_beams[x][y].get_xctr_mm()));
                        bs_ypos.setText(Double.toString(active_beams[x][y].get_yctr_mm()));
                        active_beams[x][y].set_highlighted(true);
                    } else {
                        active_beams[x][y].set_highlighted(false);
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
        String gsonified = gson.toJson(active_beams);
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
                    active_beams = gson.fromJson(input, Circle_To_Draw[][].class);
                    n_beams_x = active_beams.length;
                    if(n_beams_x>0){
                        n_beams_y = active_beams[0].length;
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

    private void UP_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UP_BUTTONActionPerformed
        double new_y = Double.parseDouble(bs_ypos.getText())+Double.parseDouble(v_nudge.getText());
        bs_ypos.setText(Double.toString(new_y));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_UP_BUTTONActionPerformed

    private void RIGHT_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RIGHT_BUTTONActionPerformed
        double new_x = Double.parseDouble(bs_xpos.getText())+Double.parseDouble(h_nudge.getText());
        bs_xpos.setText(Double.toString(new_x));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_RIGHT_BUTTONActionPerformed

    private void DOWN_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DOWN_BUTTONActionPerformed
        double new_y = Double.parseDouble(bs_ypos.getText())-Double.parseDouble(v_nudge.getText());
        bs_ypos.setText(Double.toString(new_y));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_DOWN_BUTTONActionPerformed

    private void LEFT_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LEFT_BUTTONActionPerformed
        double new_x = Double.parseDouble(bs_xpos.getText())-Double.parseDouble(h_nudge.getText());
        bs_xpos.setText(Double.toString(new_x));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_LEFT_BUTTONActionPerformed

    private void h_nudgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h_nudgeActionPerformed
        sanitise(h_nudge);
    }//GEN-LAST:event_h_nudgeActionPerformed

    private void v_nudgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_v_nudgeActionPerformed
        sanitise(v_nudge);
    }//GEN-LAST:event_v_nudgeActionPerformed

    private void show_storedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_show_storedActionPerformed
        show_stored_beams = show_stored.isSelected();
        update_beam_info();
    }//GEN-LAST:event_show_storedActionPerformed

    private void gen_square_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gen_square_buttonActionPerformed
        generate_square_beam_array();
    }//GEN-LAST:event_gen_square_buttonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton DOWN_BUTTON;
    private javax.swing.JButton LEFT_BUTTON;
    private javax.swing.JButton RIGHT_BUTTON;
    private javax.swing.JButton UP_BUTTON;
    private javax.swing.JButton add_to_list_button;
    private javax.swing.JComboBox<String> beam_selected;
    private javax.swing.JTextField bs_xpos;
    private javax.swing.JTextField bs_ypos;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JButton gen_square_button;
    private javax.swing.JButton generate_hologram_button;
    private javax.swing.JTextField h_nudge;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JButton load_button;
    private javax.swing.JTextField n_b_x_field;
    private javax.swing.JTextField n_b_y_field;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBox show_stored;
    private javax.swing.JTextField v_nudge;
    // End of variables declaration//GEN-END:variables
}
