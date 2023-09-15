/*
 * Copyright (c) 2023, Imperial College London
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

import java.io.File;
import java.nio.file.Path;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;

/**
 *
 * @author Sunil Kumar <sunil.kumar@imperial.ac.uk>
 */
public class M3M_SLM_hostframe extends javax.swing.JFrame {
    public M3M_SLM_hostframe frame_;
    public static Studio gui_ = null;
    CMMCore core_ = null;
    M3M_SLM_main uberparent_;
    File ijroot = new File(ij.IJ.getDirectory("imagej"));
    Path scriptpath;
    boolean drawing_paused = false;
    Utilities.utils2 utils = new Utilities.utils2();
    public Thread refresh_thread;
    Circle_To_Draw loom_face;
    Circle_To_Draw beams[][] = null;
        
    /**
     * Creates new form M3M_SLM_hostframe
     */
    public M3M_SLM_hostframe(Studio gui_ref) {
        frame_ = this;
        gui_ = gui_ref;
        core_ = gui_.getCMMCore();
        initComponents();
        Path scriptpath = ijroot.toPath();
        sLM_controls2.set_parent(frame_);
        excitation_map1.set_parent(frame_);
        initComponents();
        
        refresh_thread = new Thread(new DRT(this));
        refresh_thread.start();
        
        loom_face = new Circle_To_Draw("", false,18,0,0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sLM_controls1 = new M3M_SLM_UI.SLM_controls();
        excitation_map1 = new M3M_SLM_UI.Excitation_map();
        sLM_controls2 = new M3M_SLM_UI.SLM_controls();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout excitation_map1Layout = new javax.swing.GroupLayout(excitation_map1);
        excitation_map1.setLayout(excitation_map1Layout);
        excitation_map1Layout.setHorizontalGroup(
            excitation_map1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
        excitation_map1Layout.setVerticalGroup(
            excitation_map1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(sLM_controls2, javax.swing.GroupLayout.PREFERRED_SIZE, 565, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(excitation_map1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(excitation_map1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sLM_controls2, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(M3M_SLM_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(M3M_SLM_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(M3M_SLM_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(M3M_SLM_hostframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new M3M_SLM_hostframe(gui_).setVisible(true);
            }
        });
    }
    
    void setparent(M3M_SLM_main parent_frame) {
        uberparent_ = parent_frame;
    }
    
    boolean get_drawing_paused(){
        return drawing_paused;
    }
    
    void set_drawing_paused(boolean newval){
        drawing_paused = newval;
    }
    
    void set_ell(double ell_T, double ell_L, double ell_W, double ell_H) {
        excitation_map1.set_ell(ell_T, ell_L, ell_W, ell_H);
    }
    
    void refresh_panels(){
        excitation_map1.refresh();
    }

    void update_beams(Circle_To_Draw[][] beams) {
        excitation_map1.set_beams(beams);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private M3M_SLM_UI.Excitation_map excitation_map1;
    private M3M_SLM_UI.SLM_controls sLM_controls1;
    private M3M_SLM_UI.SLM_controls sLM_controls2;
    // End of variables declaration//GEN-END:variables

}
