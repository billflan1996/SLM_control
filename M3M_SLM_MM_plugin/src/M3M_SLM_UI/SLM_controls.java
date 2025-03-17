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
import exeinjava.ExeInJava;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

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
import org.micromanager.Studio;
import java.awt.image.BufferedImage;
import ij.IJ;
import ij.ImagePlus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

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
    public String file_name;
    public String row_start, row_end;
    public int column_start, column_end;
    public int FOV_p_well;
    public boolean snaked = true;
    public boolean NDD = false;
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
        
        core_ = parent_.core_;
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
    
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
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
    void create_hologram(double x, double y){
       
        double P_y, P_x, x_reps, y_reps, zr;
       
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
        float[] lens = new float[n_pixels];
        float[] totramp = new float[n_pixels];
        int xtracker = 0;
        int ytracker = 0;
        int rtracker = 0;
        for (int curr_px=0; curr_px<n_pixels;curr_px++){
            xtracker = curr_px%xres;
            ytracker = (int) Math.floor((double)curr_px/(double)xres);
            rtracker =  (int) Math.sqrt((xtracker-xres/2)*(xtracker-xres/2)+(ytracker-yres/2)*(ytracker-yres/2));
           
            xramp[curr_px] = (float) ((x_reps*xtracker/xres) * (256))%256;
            yramp[curr_px] = (float) ((y_reps*ytracker/xres) * (256))%256;
            
            
            if (P_y < 0){
                yramp[curr_px] = 256 - yramp[curr_px];
            }
           
            if (P_x < 0){
                xramp[curr_px] = 256 - xramp[curr_px];
            }
            
             
            
           
            totramp[curr_px] = (xramp[curr_px] + yramp[curr_px] + lens[curr_px])%256;
           
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
       
       
        }
       
    


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
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
        checkSLM = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        wavelength_text = new javax.swing.JTextField();
        Load = new javax.swing.JButton();
        filename = new javax.swing.JTextField();
        h_nudge = new javax.swing.JTextField();
        make_pattern = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        bal0 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        bal1 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        bal2 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        bal3 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        bal4 = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        bal5 = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        process_raws = new javax.swing.JButton();
        savepath = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        start_row = new javax.swing.JTextField();
        end_row = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        start_column = new javax.swing.JTextField();
        end_column = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        reps = new javax.swing.JTextField();
        snake_button = new javax.swing.JRadioButton();
        jLabel25 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        cmdout = new javax.swing.JTextArea();
        NDD_button = new javax.swing.JRadioButton();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

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

            v_nudge.setText("1");
            v_nudge.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    v_nudgeActionPerformed(evt);
                }
            });

            generate_hologram_button.setText("Generate hologram from active");
            generate_hologram_button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    generate_hologram_buttonActionPerformed(evt);
                }
            });

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

            checkSLM.setText("Check SLM");
            checkSLM.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    checkSLMActionPerformed(evt);
                }
            });

            jLabel10.setText("wavelength:");

            wavelength_text.setText("850");
            wavelength_text.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    wavelength_textActionPerformed(evt);
                }
            });

            Load.setText("Load Pattern");
            Load.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    LoadActionPerformed(evt);
                }
            });

            filename.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    filenameActionPerformed(evt);
                }
            });

            h_nudge.setText("1");
            h_nudge.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    h_nudgeActionPerformed(evt);
                }
            });

            make_pattern.setText("Make Pattern");
            make_pattern.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    make_patternActionPerformed(evt);
                }
            });

            jLabel13.setText("balances");

            bal0.setText("1");
            bal0.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bal0ActionPerformed(evt);
                }
            });

            jLabel14.setText("1");

            bal1.setText("1");
            bal1.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bal1ActionPerformed(evt);
                }
            });

            jLabel15.setText("2");

            bal2.setText("1");

            jLabel16.setText("4");

            bal3.setText("1");

            jLabel17.setText("3");

            bal4.setText("1");

            jLabel18.setText("5");

            bal5.setText("1");

            jLabel19.setText("6");

            process_raws.setText("Process Raws");
            process_raws.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    process_rawsActionPerformed(evt);
                }
            });

            savepath.setText("name");
            savepath.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    savepathActionPerformed(evt);
                }
            });

            jLabel11.setText("Save Folder");

            jLabel12.setText("Hologram name");

            start_row.setText("C");
            start_row.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    start_rowActionPerformed(evt);
                }
            });

            end_row.setText("F");
            end_row.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    end_rowActionPerformed(evt);
                }
            });

            jLabel20.setText("start");

            jLabel21.setText("end");

            jLabel22.setText("row");

            jLabel23.setText("column");

            start_column.setText("3");
            start_column.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    start_columnActionPerformed(evt);
                }
            });

            end_column.setText("10");
            end_column.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    end_columnActionPerformed(evt);
                }
            });

            jLabel24.setText("FOV/ well");

            reps.setText("1");
            reps.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    repsActionPerformed(evt);
                }
            });

            snake_button.setText("Snaked");
            snake_button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    snake_buttonActionPerformed(evt);
                }
            });

            jLabel25.setText("cmd out");

            cmdout.setColumns(20);
            cmdout.setRows(5);
            jScrollPane2.setViewportView(cmdout);

            NDD_button.setText("NDD");
            NDD_button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    NDD_buttonActionPerformed(evt);
                }
            });

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
            this.setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(16, 16, 16)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                            .addGap(13, 13, 13)
                                            .addComponent(jLabel10)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(wavelength_text, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(87, 87, 87)
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(DOWN_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(layout.createSequentialGroup()
                                                            .addComponent(LEFT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                            .addComponent(UP_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(RIGHT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                    .addComponent(x_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel4)
                                                        .addGap(18, 18, 18)
                                                        .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                    .addComponent(y_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addComponent(show_stored)
                                        .addComponent(clear_stored_button, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(add_to_list_button, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel6)
                                    .addGap(18, 18, 18)
                                    .addComponent(h_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel7)
                                    .addGap(18, 18, 18)
                                    .addComponent(v_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(load_button)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(save_button)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(swap_active_and_stored_button, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                                    .addGap(36, 36, 36)))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(checkSLM)
                                                .addGroup(layout.createSequentialGroup()
                                                    .addComponent(jLabel5)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                .addGroup(layout.createSequentialGroup()
                                                                    .addComponent(jLabel14)
                                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                    .addComponent(bal0))
                                                                .addComponent(jLabel13)
                                                                .addGroup(layout.createSequentialGroup()
                                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.LEADING))
                                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(bal3)
                                                                        .addComponent(bal2)
                                                                        .addComponent(bal1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addGroup(layout.createSequentialGroup()
                                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.LEADING))
                                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(bal4, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(bal5, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addComponent(jLabel12))
                                                            .addGap(54, 54, 54)
                                                            .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(layout.createSequentialGroup()
                                                            .addGap(39, 39, 39)
                                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(gen_square_button, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(generate_hologram_button, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                    .addGap(79, 79, 79))
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                    .addComponent(jLabel25)
                                                    .addGap(18, 18, 18))))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                            .addGap(0, 0, Short.MAX_VALUE)
                                            .addComponent(Load)
                                            .addGap(159, 159, 159))
                                        .addGroup(layout.createSequentialGroup()
                                            .addGap(31, 31, 31)
                                            .addComponent(make_pattern)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(193, 193, 193)
                                    .addComponent(filename, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(66, 66, 66))))
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
                            .addGap(69, 69, 69)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel8)
                                .addComponent(jLabel9))))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(52, 52, 52)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(process_raws, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(snake_button)
                                            .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jLabel20)
                                                    .addComponent(jLabel21))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel22)
                                                        .addGap(56, 56, 56)
                                                        .addComponent(jLabel23))
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addGroup(layout.createSequentialGroup()
                                                            .addComponent(end_row, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                            .addComponent(end_column, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(layout.createSequentialGroup()
                                                            .addComponent(start_row, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                            .addGap(39, 39, 39)
                                                            .addComponent(start_column, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel24)
                                                        .addGap(18, 18, 18)
                                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addComponent(NDD_button)
                                                            .addComponent(reps, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))))))))
                                .addGroup(layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel11)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(savepath, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(188, 188, 188))))
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(20, 20, 20)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(n_b_x_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1)))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel9)
                                        .addComponent(x_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel8)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(n_b_y_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel2)
                                    .addComponent(y_spacing_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(29, 29, 29)
                            .addComponent(checkSLM))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(20, 20, 20)
                            .addComponent(generate_hologram_button)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(gen_square_button)))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(beam_selected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3)
                                .addComponent(jLabel4)
                                .addComponent(bs_xpos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5)
                                .addComponent(bs_ypos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(35, 35, 35)
                            .addComponent(jLabel25)))
                    .addGap(38, 38, 38)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel13)
                            .addGap(12, 12, 12)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel14)
                                .addComponent(bal0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel15)
                                .addComponent(bal1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(bal2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel17))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel16)
                                .addComponent(bal3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel18)
                                .addComponent(bal4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel19)
                                .addComponent(bal5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel12)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(filename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(Load))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(h_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(1, 1, 1)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(v_nudge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(UP_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(wavelength_text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel10))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(add_to_list_button)
                                        .addComponent(DOWN_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(clear_stored_button)
                                    .addGap(18, 18, 18)
                                    .addComponent(show_stored)
                                    .addGap(30, 30, 30)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(load_button)
                                        .addComponent(save_button)
                                        .addComponent(swap_active_and_stored_button)
                                        .addComponent(make_pattern)))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addGap(76, 76, 76)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(LEFT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(RIGHT_BUTTON, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                            .addGap(75, 75, 75)
                                            .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGap(0, 0, Short.MAX_VALUE)))
                            .addContainerGap())))
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSeparator1))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(30, 30, 30)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel11)
                        .addComponent(savepath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel23)
                        .addComponent(jLabel22))
                    .addGap(13, 13, 13)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(start_row, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel20)
                        .addComponent(start_column, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(34, 34, 34)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(end_row, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel21)
                        .addComponent(end_column, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel24)
                        .addComponent(reps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(snake_button)
                        .addComponent(NDD_button))
                    .addGap(31, 31, 31)
                    .addComponent(process_raws)
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
            String sel_beam = (String) beam_selected.getSelectedItem();
            for(int i=0;i<n_active_beams;i++){
                if(active_beams[i].get_name()==sel_beam){
                    bs_xpos.setText(Double.toString(active_beams[i].get_xctr_mm()));
                    bs_ypos.setText(Double.toString(active_beams[i].get_yctr_mm()));
                    active_beams[i].set_highlighted(true);
                } else {
                    active_beams[i].set_highlighted(false);
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
        String result = String.format("%.2f", new_y);
        bs_ypos.setText(result);
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_UP_BUTTONActionPerformed

    private void RIGHT_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RIGHT_BUTTONActionPerformed
        double new_x = Double.parseDouble(bs_xpos.getText())+Double.parseDouble(h_nudge.getText());
        String result = String.format("%.2f", new_x);
        bs_xpos.setText(result);
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_RIGHT_BUTTONActionPerformed

    private void DOWN_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DOWN_BUTTONActionPerformed
        double new_y = Double.parseDouble(bs_ypos.getText())-Double.parseDouble(v_nudge.getText());
        String result = String.format("%.2f", new_y);
        bs_ypos.setText(result);
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_DOWN_BUTTONActionPerformed

    private void LEFT_BUTTONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LEFT_BUTTONActionPerformed
        double new_x = Double.parseDouble(bs_xpos.getText())-Double.parseDouble(h_nudge.getText());
        String result = String.format("%.2f", new_x);
        bs_xpos.setText(result); 
        String beam_name = beam_selected.getSelectedItem().toString();
        update_beam_details(beam_name,Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
        
        update_SLM(Double.parseDouble(bs_xpos.getText()),Double.parseDouble(bs_ypos.getText()));
    }//GEN-LAST:event_LEFT_BUTTONActionPerformed

    private void v_nudgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_v_nudgeActionPerformed
        sanitise(v_nudge);
    }//GEN-LAST:event_v_nudgeActionPerformed

    private void show_storedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_show_storedActionPerformed
        show_stored_beams = show_stored.isSelected();
        update_beam_arrays();
    }//GEN-LAST:event_show_storedActionPerformed

    private void gen_square_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gen_square_buttonActionPerformed
        generate_square_beam_array();
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

    private void checkSLMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkSLMActionPerformed
        check_SLM_status();
    }//GEN-LAST:event_checkSLMActionPerformed

    private void wavelength_textActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wavelength_textActionPerformed
        sanitise(wavelength_text);
        lambda = Double.parseDouble(wavelength_text.getText())*nm;
        
        
    }//GEN-LAST:event_wavelength_textActionPerformed

    private void LoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadActionPerformed
       
        try {                                     
            
            String filePath = "D:\\m3mbill\\python_slm_code\\";
            filePath = filePath + file_name;
            System.out.println(filePath);
            File file = new File(filePath);
            System.out.println(1);
            BufferedImage image = ImageIO.read(file);//here i have the image
            
            ColorProcessor colorProcessor = new ColorProcessor(image);

        // Convert ColorProcessor to ImageProcessor (grayscale)
            ImageProcessor ip = colorProcessor.convertToByteProcessor();
        
            //ImageProcessor ip = new ByteProcessor(image);
            System.out.println(2);
            try {
                //core_.setSLMImage(SLM_dev, (byte [])ip.getPixels());
                core_.setSLMImage(SLM_dev, (int [])colorProcessor.getPixels());
                core_.displaySLMImage(SLM_dev);
            } catch (Exception ex) {
                Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
        } catch (IOException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
       
       
        
    }//GEN-LAST:event_LoadActionPerformed

    private void filenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filenameActionPerformed
        
        file_name = filename.getText();
    }//GEN-LAST:event_filenameActionPerformed

    private void h_nudgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h_nudgeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_h_nudgeActionPerformed

    private void generate_hologram_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generate_hologram_buttonActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_generate_hologram_buttonActionPerformed

    private void make_patternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_make_patternActionPerformed
        
        double a = default_beamspacing_h;
        double b= default_beamspacing_v;
        int pat = 1; // 1 = grid, 0 = line
        double[][] positions = new double[6][2];
        double[] bals = new double[6];

        if (pat == 1) { // For known spacing
            positions[0][0] = -a / 2; positions[0][1] = b;
            positions[1][0] = -a / 2; positions[1][1] = 0;
            positions[2][0] = -a / 2; positions[2][1] = -b;
            positions[3][0] = a / 2; positions[3][1] = b;
            positions[4][0] = a / 2; positions[4][1] = 0;
            positions[5][0] = a / 2; positions[5][1] = -b;
        }
        // If pat == 0 .....

        // Read balance values from text fields
        bals[0] = Double.parseDouble(bal0.getText());
        bals[1] = Double.parseDouble(bal1.getText());
        bals[2] = Double.parseDouble(bal2.getText());
        bals[3] = Double.parseDouble(bal3.getText());
        bals[4] = Double.parseDouble(bal4.getText());
        bals[5] = Double.parseDouble(bal5.getText());

        // Get the lambda (wavelength) value
        String lambdastring = String.valueOf(lambda); 

        // Build the positions string for the command
        StringBuilder positionsString = new StringBuilder();
        for (int i = 0; i < positions.length; i++) {
            positionsString.append(positions[i][0]).append(" ").append(positions[i][1]);
            if (i < positions.length - 1) {
                positionsString.append(" "); // Separate each position with a space
            }
        }

        // Build the balances string
        StringBuilder balsString = new StringBuilder();
        for (int i = 0; i < bals.length; i++) {
            balsString.append(bals[i]);
            if (i < bals.length - 1) {
                balsString.append(" "); // Separate each balance value with a space
            }
        }

        // Combine all arguments into a single command string
        String command = String.format("python quickGStest.py --wavelength %s --positions %s --balance %s",
                                        lambdastring, positionsString.toString(), balsString.toString());

        // Output the prepared command
        
        
        
        ExeInJava cmds = new ExeInJava();
        cmds.startCMD();
        
         
        
        cmds.WriteCommands("C:\\ProgramData\\Anaconda3\\scripts\\activate");
        
        
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        cmds.WriteCommands("conda activate test3");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        cmds.WriteCommands("d:");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        
        
        cmds.WriteCommands("cd " + "D:\\m3mbill\\python_slm_code");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        System.out.println(command);
        cmds.WriteCommands(command); // 
        //cmds.WriteCommands("python quickGStest.py --wavelength 115 --positions -4.4 5.7 -4.4 0 -4.4 -5.7 4.4 5.7 4.4 0 4.4 -5.7 --balance 1 1 1 1 1 1"); // Run the command
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        filename.setText("a"+String.valueOf(lambda/nm)+".bmp");
        
        // Step 1: Create the balances string (same as Python join with '_')
        StringBuilder balsStr = new StringBuilder();
        for (int i = 0; i < bals.length; i++) {
            if (i > 0) {
                balsStr.append("_");
            }
            balsStr.append(bals[i]);
        }
        // Step 2: Take the first position (x, y)
        double firstX = positions[0][0];
        double firstY = positions[0][1];

        // Step 3: Create the positions string (only the first (x, y) pair)
        String positionsStr = firstX + "_" + firstY;

        // Step 4: Construct the filename
        String filenametext = "a" + (lambda / nm) + "_" + balsStr.toString() + "_" + positionsStr.toString() + ".bmp";

        // Step 5: Set the filename in a JTextField or JLabel
        filename.setText(filenametext);  // Assuming filenameLabel is a JTextField or JLabel
        
        // Optionally, you can print the filename to check
        System.out.println(filename);
        //cmd_output.setText(cmds.GetLatestOutput());
        cmds.stopCMD(100);

        
        
    }//GEN-LAST:event_make_patternActionPerformed

    private void bal0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bal0ActionPerformed
        Double.parseDouble(bal0.getText());
    }//GEN-LAST:event_bal0ActionPerformed

    private void bal1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bal1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_bal1ActionPerformed

    private void process_rawsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_process_rawsActionPerformed
        
        ExeInJava cmds = new ExeInJava();
        cmds.startCMD();
        cmds.WriteCommands("C:\\ProgramData\\Anaconda3\\scripts\\activate");
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        cmds.WriteCommands("conda activate billyenv");
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        cmds.WriteCommands("d:");
        cmds.WriteCommands("cd " + "D:\\python prgs");
        String path = savepath.getText();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Change format as needed
        String formattedDate = currentDate.format(formatter);

        // Prepend the date to the path
        String updatedPath = formattedDate + "_" + path;
        //cmds.WriteCommands("python arg_flim_processor2.py --folder "+updatedPath);//put args here, change script to accept args, should just be file folder name.
        cmds.WriteCommands("python -u arg_flim_processor3.py --folder " + updatedPath +
        " --row_start " + row_start +
        " --row_end " + row_end +
        " --column_start " + column_start +
        " --column_end " + column_end +
        " --FOV_p_well " + FOV_p_well +
        " --snake " + (snaked ? "1" : "0") +
        " --NDD " + (NDD ? "1" : "0"));


//        while (true) {
//            String str = cmds.GetLatestOutput();
//            if (str != null && str.startsWith("fin")) {
//                System.out.println(cmds.GetLatestOutput());
//                savepath.setText("Done!");
//                break;
//            } else {
//                try {
//                    savepath.setText(str);
//                    Thread.sleep(100);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }
        new Thread(() -> {
        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null && str.startsWith("fin")) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println(str);
                    savepath.setText("Done!");
                });
                break;
            } else {
                try {
                    System.out.println("Debug: str = " + str);
                    SwingUtilities.invokeLater(() -> cmdout.setText(str));
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SLM_controls.class.getName()).log(Level.SEVERE, null, ex);
        }
        
                // Load the TIFF file
        String filePath = "D:\\m3mbill\\m3mdata\\"+updatedPath+"\\intensitystack.tif";  // Update this to your actual file path

        // Use ImageJ to open the TIFF file
        ImagePlus image = IJ.openImage(filePath);

        // Check if the image was successfully loaded
        if (image != null) {
            // Show the image in an ImageJ window
            image.show();
        } else {
            System.out.println("Failed to open image: " + filePath);
        }
        cmds.stopCMD(100);
    }).start();

        
        

        
        
        
    }//GEN-LAST:event_process_rawsActionPerformed

    private void savepathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savepathActionPerformed
        
    }//GEN-LAST:event_savepathActionPerformed

    private void start_columnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_start_columnActionPerformed
        column_start = Integer.parseInt(start_column.getText());
    }//GEN-LAST:event_start_columnActionPerformed

    private void start_rowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_start_rowActionPerformed
        row_start = start_row.getText();
    }//GEN-LAST:event_start_rowActionPerformed

    private void end_rowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_end_rowActionPerformed
        row_end = end_row.getText();
    }//GEN-LAST:event_end_rowActionPerformed

    private void end_columnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_end_columnActionPerformed
        column_end = Integer.parseInt(end_column.getText());
    }//GEN-LAST:event_end_columnActionPerformed

    private void repsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repsActionPerformed
        FOV_p_well = Integer.parseInt(reps.getText());
    }//GEN-LAST:event_repsActionPerformed

    private void snake_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snake_buttonActionPerformed
        snaked = snake_button.isSelected();
    }//GEN-LAST:event_snake_buttonActionPerformed

    private void NDD_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NDD_buttonActionPerformed
        NDD = NDD_button.isSelected();
    }//GEN-LAST:event_NDD_buttonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton DOWN_BUTTON;
    private javax.swing.JButton LEFT_BUTTON;
    private javax.swing.JButton Load;
    private javax.swing.JRadioButton NDD_button;
    private javax.swing.JButton RIGHT_BUTTON;
    private javax.swing.JButton UP_BUTTON;
    private javax.swing.JButton add_to_list_button;
    private javax.swing.JTextField bal0;
    private javax.swing.JTextField bal1;
    private javax.swing.JTextField bal2;
    private javax.swing.JTextField bal3;
    private javax.swing.JTextField bal4;
    private javax.swing.JTextField bal5;
    private javax.swing.JComboBox<String> beam_selected;
    private javax.swing.JTextField bs_xpos;
    private javax.swing.JTextField bs_ypos;
    private javax.swing.JButton checkSLM;
    private javax.swing.JButton clear_stored_button;
    private javax.swing.JTextArea cmdout;
    private javax.swing.JTextField end_column;
    private javax.swing.JTextField end_row;
    private javax.swing.JTextField filename;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JButton gen_square_button;
    private javax.swing.JButton generate_hologram_button;
    private javax.swing.JTextField h_nudge;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JButton load_button;
    private javax.swing.JButton make_pattern;
    private javax.swing.JTextField n_b_x_field;
    private javax.swing.JTextField n_b_y_field;
    private javax.swing.JButton process_raws;
    private javax.swing.JTextField reps;
    private javax.swing.JButton save_button;
    private javax.swing.JTextField savepath;
    private javax.swing.JCheckBox show_stored;
    private javax.swing.JRadioButton snake_button;
    private javax.swing.JTextField start_column;
    private javax.swing.JTextField start_row;
    private javax.swing.JButton swap_active_and_stored_button;
    private javax.swing.JTextField v_nudge;
    private javax.swing.JTextField wavelength_text;
    private javax.swing.JTextField x_spacing_field;
    private javax.swing.JTextField y_spacing_field;
    // End of variables declaration//GEN-END:variables

}
