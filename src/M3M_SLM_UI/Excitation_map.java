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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

/**
 *
 * @author kumars2
 */

public class Excitation_map extends javax.swing.JPanel implements MouseListener{
    private M3M_SLM_hostframe parent_;
    Stroke thin_stroke = new BasicStroke((float) 0.1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0);
    Stroke thick_stroke = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0);
    Stroke heavy_stroke = new BasicStroke(40, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0);
    Shape render_shape = new Rectangle(); 
    double panelwidth;
    double panelheight;
    double draw_scaling;
    double padfraction = 0.0;
    double loom_face_diam = 18;
    AffineTransform a_t = new AffineTransform();

    double ell_T = -5;
    double ell_L = -5;
    double ell_W = 10;
    double ell_H = 10;
    
    Circle_To_Draw loom_face;
    Circle_To_Draw beams[][] = null;
    
    /**
     * Creates new form Excitation_map
     */
    public Excitation_map() {
        addMouseListener(this);
        initComponents();
    }

    public void set_parent(Object parentframe){
        parent_ = (M3M_SLM_hostframe) parentframe;
        parent_.set_drawing_paused(false);
        init_canvas();
    }
    
    public void calc_scaling(){
        panelwidth = this.getWidth();
        panelheight = this.getHeight();
        draw_scaling = (panelwidth*(1-(2*padfraction))/(loom_face_diam));
    }    
    
    public void init_canvas() {
        setBackground(Color.white);
        setForeground(Color.white);
    }
        
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);  
        if(null == parent_.gui_){
            System.out.println("Null parent");
        } else {//if (false==parent_.get_drawing_paused())
            Graphics2D map = (Graphics2D)g;
            calc_scaling();
            map.clearRect(0,0,(int)panelwidth,(int)panelheight);
            map.setStroke(thin_stroke);
            map.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            map.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            map.setPaint(Color.black);
            
            //Set transform
            a_t.setToIdentity();
            a_t.translate(getWidth()/2,getHeight()/2);
            
            a_t.scale(draw_scaling,draw_scaling);
            map.setTransform(a_t);
            
            render_shape = new Ellipse2D.Double(ell_L,ell_T,ell_W,ell_H);
            map.draw(render_shape);
            
            if(beams!=null){
                for(int x=0;x<beams.length;x++){
                    for(int y=0;y<beams[x].length;y++){
                        map.setPaint(Color.red);
                        System.out.println(beams[x][y].get_xctr_mm()+","+beams[x][y].get_yctr_mm());
                        render_shape = new Ellipse2D.Double((beams[x][y].get_xctr_mm()-beams[x][y].get_rad_mm()),(beams[x][y].get_yctr_mm()-beams[x][y].get_rad_mm()),beams[x][y].get_rad_mm()*2,beams[x][y].get_rad_mm()*2);
                        map.fill(render_shape);
                    }
                }
            }
        } //else {
//            System.out.println("Drawing paused");
//        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    void set_ell(double ell_T_in, double ell_L_in, double ell_W_in, double ell_H_in) {
        ell_T = ell_T_in;
        ell_L = ell_L_in;
        ell_W = ell_W_in;
        ell_H = ell_H_in;
        System.out.println(ell_T+", "+ell_L+", "+ell_W+", "+ell_H);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("Click");
        refresh();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        System.out.println("Press");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        System.out.println("Release");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        System.out.println("Enter");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        System.out.println("Exit");
    }

    void refresh() {
        repaint();
    }

    void set_beams(Circle_To_Draw[][] beams_in) {
        beams = beams_in;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
