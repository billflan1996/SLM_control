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
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
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
import mmcorej.CMMCore;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

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
    public int n_active_beams = n_beams_x*n_beams_y;
    Circle_To_Draw[]active_beams = new Circle_To_Draw[n_beams_x*n_beams_y];
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
    boolean show_stored_beams = true;
    
    public String SLM_dev;
    private CMMCore core_ = null;
    public double mm = 0.001, um = 0.000001, nm = 0.000000001;
    public double lambda = 850*nm;
    public double f = 300*mm;
    public double ap = 9.6*mm;
    
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
        clear_stored_beams();
        
        core_ = parent_.core_;//need to change this to do same as other one, Sunil probably knows best way
        check_SLM_status();
    }    
    
    void setup_for_display(){
        generate_square_beam_array();
    }
    
    void sanitise(JTextField source_field){
        String input = source_field.getText();
        source_field.setText(parent_.utils.strip_non_numeric(input));
    }
       
    void update_beam_arrays(){
        Circle_To_Draw[] truncated_stored_beams = Arrays.copyOfRange(stored_beams, 0, current_stored_beam);
        parent_.update_beams(active_beams,truncated_stored_beams,show_stored_beams);
    }
    
    private void clear_stored_beams() {
        stored_beams = new Circle_To_Draw[max_stored_beams];
        int i=0;
        for(Circle_To_Draw beam : stored_beams){
            stored_beams[i] = new Circle_To_Draw();
            i++;
        }
        current_stored_beam = 0;
        update_beam_arrays();
    }
    
    void update_beam_details(String beam_name,double new_xpos, double new_ypos){
        for(int i=0;i<n_active_beams;i++){
            if(active_beams[i].get_name()==beam_name){
                active_beams[i].set_xctr_mm(new_xpos);
                active_beams[i].set_yctr_mm(new_ypos);
            }
        }
        update_beam_arrays();
    }
    
    
    void create_hologram(double x, double y){
        
        double P_y, P_x, x_reps, y_reps;
        
        //input numbers are in mm
        x = x*mm;
        y = y*mm;
        
        P_y = Math.PI * y * ap / (2*lambda * f);
        P_x = Math.PI * x * ap / (2*lambda * f);

        x_reps = Math.abs(P_x);
        y_reps = Math.abs(P_y);

        

        //IJ.newImage("beam" + (String) beam_selected.getSelectedItem() + "", "8-bit white", 1920, 1200, 1);
        
        //x_val and y_val tell you how many ramps there are, currently using imageJ's image maker using a custom function
        String x_val = x_reps + "";
        String y_val = y_reps + "";
        
        
        //v=(" + x_val + "*(x)*256/w+" + y_val + "*(y)*256/w)%256")
        
        int xres = 1920;
        int yres = 1200;
        int n_pixels = xres*yres;
        float[] xramp = new float[n_pixels];
        float[] yramp = new float[n_pixels];
        float[] totramp = new float[n_pixels];
        int xtracker = 0;
        int ytracker = 0;
        for (int curr_px=0; curr_px<n_pixels;curr_px++){
            xtracker = curr_px%xres;
            ytracker = (int) Math.floor((double)curr_px/(double)xres);
            
            
            xramp[curr_px] = (float) ((x_reps*xtracker/yres) * (256))%256;
            yramp[curr_px] = (float) ((y_reps*ytracker/yres) * (256))%256;
            
            if (P_y < 0){
                yramp[curr_px] = 256 - yramp[curr_px];
            }
            
            if (P_x < 0){
                xramp[curr_px] = 256 - xramp[curr_px];
            }
            
            
            
            totramp[curr_px] = (xramp[curr_px] + yramp[curr_px])%256;
            
        }
        FloatProcessor ip = new FloatProcessor(xres, yres);
        ip.setPixels(totramp);
        ByteProcessor bp = ip.convertToByteProcessor(true);
        try {
            core_.setSLMImage(SLM_dev, (byte [])bp.getPixels());
            core_.displaySLMImage(SLM_dev);
        } catch (Exception ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //System.out.println(x_val +" and " + y_val);

//        if (y > 0) {
//
//            if (x >= 0) {
//                IJ.run("Macro...", "code=v=(" + x_val + "*(x)*256/w+" + y_val + "*(y)*256/w)%256");
//            }
//            if (x < 0) {
//                IJ.run("Macro...", "code=v=(" + x_val + "*(w-x)*256/w+" + y_val + "*(y)*256/w)%256");
//            }
//
//        }
//        if (y < 0) {
//
//            if (x >= 0) {
//                IJ.run("Macro...", "code=v=(" + x_val + "*(x)*256/w+" + y_val + "*(h-y)*256/w)%256");
//            }
//            if (x < 0) {
//                IJ.run("Macro...", "code=v=(" + x_val + "*(w-x)*256/w+" + y_val + "*(h-y)*256/w)%256");
//            }
//
//        }
        
    }
    
    
     private void set_SLM() {
        try {
            ImagePlus imp = IJ.getImage();
            ImageProcessor improc = imp.getProcessor();
            byte[] px1 = (byte[]) improc.getPixels();
            core_.setSLMImage(SLM_dev, px1);
            core_.displaySLMImage(SLM_dev);
            imp.changes = false;
            //imp.close();
        } catch (Exception ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    void update_SLM(double x,double y){
    
        create_hologram(x, y);
        //set_SLM();
        
        
    }
    
    void check_SLM_status(){
        try {
            SLM_dev = core_.getSLMDevice();
            long SLM_bpp = core_.getSLMBytesPerPixel(SLM_dev);
            long SLM_width = core_.getSLMWidth(SLM_dev);
            long SLM_height = core_.getSLMHeight(SLM_dev);
            System.out.println(SLM_dev + " at [" + SLM_width + " x " + SLM_height + "], with " + SLM_bpp + " bytes per pixel");
        } catch (Exception ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void generate_square_beam_array(){
        n_active_beams = n_beams_x*n_beams_y;
        active_beams = new Circle_To_Draw[n_active_beams];
        int i=0;
        for(int x=0;x<n_beams_x;x++){
            for(int y=0;y<n_beams_y;y++){
                String name = "Beam_"+Integer.toString(i);
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
                active_beams[i] = new Circle_To_Draw(name,true, 0.5,(x*x_spacing)-(range_x/2)-offset_x,(y*y_spacing)-(range_y/2)-offset_y,colour_list[(y+(x*n_beams_y))%n_colours],false);
                i++;
            }
        }
        update_dropdown_from_beams();
        if(initialised_){
            update_beam_arrays();
        }
    }
    
    void update_dropdown_from_beams(){
        ignore_selection_activity = true;
        beam_selected.removeAllItems();
        int to_highlight = 0;
        double sel_x_pos = active_beams[0].get_xctr_mm();
        double sel_y_pos = active_beams[0].get_yctr_mm();
        boolean any_highlighted = false;
        for(int i=0;i<n_active_beams;i++){
            beam_selected.addItem(active_beams[i].get_name());
            if(active_beams[i].get_highlighted()){
                to_highlight = i;
                sel_x_pos = active_beams[i].get_xctr_mm();
                sel_y_pos = active_beams[i].get_yctr_mm();
                any_highlighted = true;
            }
        }
        if(!any_highlighted){
            active_beams[0].set_highlighted(true);
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
        swap_active_and_stored_button = new javax.swing.JButton();
        clear_stored_button = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        y_spacing_field = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        x_spacing_field = new javax.swing.JTextField();
        lambda_field = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        status_field = new javax.swing.JButton();

        add_to_list_button.setText("Add to stored");
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

        load_button.setText("Load to stored");
        load_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                load_buttonActionPerformed(evt);
            }
        });

        save_button.setText("Save stored pattern");
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

            h_nudge.setText("1");
            h_nudge.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    h_nudgeActionPerformed(evt);
                }
            });

            v_nudge.setText("1");
            v_nudge.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    v_nudgeActionPerformed(evt);
                }
            });

            generate_hologram_button.setText("Generate hologram from active");

            jLabel6.setText("h nudge");

            jLabel7.setText("v nudge");

            show_stored.setSelected(true);
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

            swap_active_and_stored_button.setText("Swap active/stored");
            swap_active_and_stored_button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    swap_active_and_stored_buttonActionPerformed(evt);
                }
            });

            clear_stored_button.setText("Clear stored");
            clear_stored_button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    clear_stored_buttonActionPerformed(evt);
                }
            });

            jLabel8.setText("y spacing:");

            y_spacing_field.setText("3");
            y_spacing_field.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    y_spacing_fieldActionPerformed(evt);
                }
            });

            jLabel9.setText("x spacing:");

            x_spacing_field.setText("4");
            x_spacing_field.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    x_spacing_fieldActionPerformed(evt);
                }
            });

            lambda_field.setText("850");
            lambda_field.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    lambda_fieldActionPerformed(evt);
                }
            });

            jLabel10.setText("λ/ nm :");

            status_field.setText("SLM status");
            status_field.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    status_fieldActionPerformed(evt);
                }
            });

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
            this.setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(load_button)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(save_button)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(swap_active_and_stored_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(226, 226, 226)
                                    .addComponent(jLabel4)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
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
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel8)
                                                .addComponent(jLabel9))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(x_spacing_field)
                                    .addComponent(y_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(18, 18, 18)
                            .addComponent(jLabel5)
                            .addGap(18, 18, 18)
                            .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addContainerGap(60, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lambda_field, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addContainerGap())))
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
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(status_field)
                            .addGap(33, 33, 33)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(gen_square_button, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(generate_hologram_button, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(12, 12, 12))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(16, 16, 16)
                            .addComponent(show_stored)
                            .addGap(43, 43, 43)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(clear_stored_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(add_to_list_button, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(LEFT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                        .addComponent(UP_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(DOWN_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel10))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(RIGHT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(filler2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(38, 38, 38))))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(19, 19, 19)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(n_b_x_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1)
                                .addComponent(generate_hologram_button)
                                .addComponent(jLabel9)
                                .addComponent(x_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(n_b_y_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2)
                                .addComponent(jLabel8)
                                .addComponent(y_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(gen_square_button)))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(34, 34, 34)
                            .addComponent(status_field)))
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(beam_selected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)
                        .addComponent(jLabel4)
                        .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5)
                        .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(h_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel6))
                                .addComponent(add_to_list_button))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(v_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel7)
                                .addComponent(clear_stored_button))
                            .addGap(18, 18, 18)
                            .addComponent(show_stored)
                            .addGap(30, 30, 30)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(load_button)
                                .addComponent(save_button)
                                .addComponent(swap_active_and_stored_button)
                                .addComponent(lambda_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel10))
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(UP_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(RIGHT_BUTTON, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(LEFT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(DOWN_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(59, 59, 59)))))
                    .addContainerGap())
            );
        }// </editor-fold>//GEN-END:initComponents

    private void add_to_list_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_to_list_buttonActionPerformed
        if(current_stored_beam<max_stored_beams){
            stored_beams[current_stored_beam] = new Circle_To_Draw(active_beams[0]);
            stored_beams[current_stored_beam].set_name("Beam_"+Integer.toString(current_stored_beam));
            stored_beams[current_stored_beam].set_colour(colour_list[current_stored_beam%n_colours]);
            current_stored_beam++;
            update_beam_arrays();
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
            String sel_beam = (String) beam_selected.getSelectedItem() ;
            for(int i=0;i<n_active_beams;i++){
                if(active_beams[i].get_name()==sel_beam){
                    bs_xpos.setText(Double.toString(active_beams[i].get_xctr_mm()));
                    bs_ypos.setText(Double.toString(active_beams[i].get_yctr_mm()));
                    active_beams[i].set_highlighted(true);
                } else {
                    active_beams[i].set_highlighted(false);
                    }
            }
            
            update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
            
            
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
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_bs_xposActionPerformed

    private void bs_yposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bs_yposActionPerformed
        sanitise(bs_ypos);
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_bs_yposActionPerformed

    private void save_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_buttonActionPerformed
        Circle_To_Draw[] tmp_stored = Arrays.copyOfRange(stored_beams, 0, current_stored_beam);
        String gsonified = gson.toJson(tmp_stored);
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
                    stored_beams = gson.fromJson(input, Circle_To_Draw[].class);
                    current_stored_beam = stored_beams.length;
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            } else {
                System.out.println("NOT AN EXISTING FILE?!");
            }
            update_beam_arrays();
        }
    }//GEN-LAST:event_load_buttonActionPerformed

    private void UP_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UP_BUTTONActionPerformed
        double new_y = Double.parseDouble(bs_ypos.getText())+Double.parseDouble(v_nudge.getText());
        bs_ypos.setText(Double.toString(new_y));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_UP_BUTTONActionPerformed

    private void RIGHT_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RIGHT_BUTTONActionPerformed
        double new_x = Double.parseDouble(bs_xpos.getText())+Double.parseDouble(h_nudge.getText());
        bs_xpos.setText(Double.toString(new_x));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_RIGHT_BUTTONActionPerformed

    private void DOWN_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DOWN_BUTTONActionPerformed
        double new_y = Double.parseDouble(bs_ypos.getText())-Double.parseDouble(v_nudge.getText());
        bs_ypos.setText(Double.toString(new_y));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_DOWN_BUTTONActionPerformed

    private void LEFT_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LEFT_BUTTONActionPerformed
        double new_x = Double.parseDouble(bs_xpos.getText())-Double.parseDouble(h_nudge.getText());
        bs_xpos.setText(Double.toString(new_x));
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_LEFT_BUTTONActionPerformed

    private void h_nudgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h_nudgeActionPerformed
        sanitise(h_nudge);
    }//GEN-LAST:event_h_nudgeActionPerformed

    private void v_nudgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_v_nudgeActionPerformed
        sanitise(v_nudge);
    }//GEN-LAST:event_v_nudgeActionPerformed

    private void show_storedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_show_storedActionPerformed
        show_stored_beams = show_stored.isSelected();
        update_beam_arrays();
    }//GEN-LAST:event_show_storedActionPerformed

    private void gen_square_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gen_square_buttonActionPerformed
        generate_square_beam_array();
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_gen_square_buttonActionPerformed

    private void clear_stored_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_stored_buttonActionPerformed
        clear_stored_beams();
    }//GEN-LAST:event_clear_stored_buttonActionPerformed

    private void swap_active_and_stored_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_swap_active_and_stored_buttonActionPerformed
        if(stored_beams != null){
            if(stored_beams[0].get_name() != null && active_beams != null){
                Circle_To_Draw[] tmp_stored = stored_beams.clone();
                stored_beams = new Circle_To_Draw[max_stored_beams];
                int i=0;
                for(Circle_To_Draw beam : active_beams){
                    if(i<max_stored_beams){
                        stored_beams[i] = new Circle_To_Draw(beam);
                        i++;
                    } else {
                        System.out.println("Too many stored beams!");
                    }
                }
                i=0;
                active_beams = new Circle_To_Draw[current_stored_beam];
                for(Circle_To_Draw beam : tmp_stored){
                    if(i<current_stored_beam){
                        active_beams[i] = new Circle_To_Draw(beam);
                        i++;
                    } else {
                        //No need to worry about these
                    }
                }
                current_stored_beam = n_active_beams;
                n_active_beams = active_beams.length;
                update_dropdown_from_beams();
                update_beam_arrays();
            }    
        }
    }//GEN-LAST:event_swap_active_and_stored_buttonActionPerformed

    private void x_spacing_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_x_spacing_fieldActionPerformed
        sanitise(x_spacing_field);
        default_beamspacing_h = Double.parseDouble(x_spacing_field.getText());
    }//GEN-LAST:event_x_spacing_fieldActionPerformed

    private void y_spacing_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_y_spacing_fieldActionPerformed
        sanitise(y_spacing_field);
        default_beamspacing_v = Double.parseDouble(y_spacing_field.getText());
    }//GEN-LAST:event_y_spacing_fieldActionPerformed

    private void lambda_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lambda_fieldActionPerformed
        sanitise(lambda_field);
        lambda = Double.parseDouble(lambda_field.getText())*nm;
    }//GEN-LAST:event_lambda_fieldActionPerformed

    private void status_fieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_status_fieldActionPerformed
        check_SLM_status();
    }//GEN-LAST:event_status_fieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton DOWN_BUTTON;
    private javax.swing.JButton LEFT_BUTTON;
    private javax.swing.JButton RIGHT_BUTTON;
    private javax.swing.JButton UP_BUTTON;
    private javax.swing.JButton add_to_list_button;
    private javax.swing.JComboBox<String> beam_selected;
    private javax.swing.JTextField bs_xpos;
    private javax.swing.JTextField bs_ypos;
    private javax.swing.JButton clear_stored_button;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JButton gen_square_button;
    private javax.swing.JButton generate_hologram_button;
    private javax.swing.JTextField h_nudge;
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
    private javax.swing.JTextField lambda_field;
    private javax.swing.JButton load_button;
    private javax.swing.JTextField n_b_x_field;
    private javax.swing.JTextField n_b_y_field;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBox show_stored;
    private javax.swing.JButton status_field;
    private javax.swing.JButton swap_active_and_stored_button;
    private javax.swing.JTextField v_nudge;
    private javax.swing.JTextField x_spacing_field;
    private javax.swing.JTextField y_spacing_field;
    // End of variables declaration//GEN-END:variables

}
