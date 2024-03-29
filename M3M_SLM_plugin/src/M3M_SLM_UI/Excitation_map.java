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
    Stroke thin_dashed_stroke = new BasicStroke((float) 0.2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,0, new float[]{0.314f}, 0);
    Stroke medium_stroke = new BasicStroke((float) 0.5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0);
    Stroke thick_stroke = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0);
    Stroke heavy_stroke = new BasicStroke(40, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0);
    Shape render_shape = new Rectangle(); 
    double panelwidth;
    double panelheight;
    double draw_scaling;
    double padfraction = 0.0;
    double loom_face_diam = 18;
    AffineTransform a_t = new AffineTransform();
    int blink_counter = 0;
    int blink_stride = 10;
    boolean blink_on = false;
    boolean show_stored_beams = true;

    Circle_To_Draw loom_face = new Circle_To_Draw("Loom face",false,loom_face_diam/2,0,0, Color.black, false);
    Circle_To_Draw[] active_beams = null;
    Circle_To_Draw stored_beams[] = null;
    int n_stored_beams = 0;
    
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
        draw_scaling = (panelwidth*(1-(2*padfraction))/(2*loom_face_diam));
    }    
    
    public void init_canvas() {
        setBackground(Color.white);
        setForeground(Color.white);
    }
        
    @Override
    public void paintComponent(Graphics g) {
        blink_counter++;
        if(blink_counter%blink_stride == 0){
            blink_on = !blink_on;
        }
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
            //Centre at zero
            a_t.translate(getWidth()/2,getHeight()/2);
            //Flip vertical
            a_t.scale(draw_scaling,-draw_scaling);
            map.setTransform(a_t);
            
            //Loom area render
            map.setStroke(medium_stroke);
            render_shape = new Ellipse2D.Double((loom_face.get_xctr_mm()-loom_face.get_rad_mm()),(loom_face.get_yctr_mm()-loom_face.get_rad_mm()),loom_face.get_rad_mm()*2,loom_face.get_rad_mm()*2);
            map.draw(render_shape);
            
            //Active beams render
            if(active_beams!=null){
                if(active_beams.length>0){
                    for(int i=0;i<active_beams.length;i++){
                        map.setPaint(active_beams[i].get_colour());
                        //System.out.println(beams[x][y].get_xctr_mm()+","+beams[x][y].get_yctr_mm());
                        render_shape = new Ellipse2D.Double((active_beams[i].get_xctr_mm()-active_beams[i].get_rad_mm()),(active_beams[i].get_yctr_mm()-active_beams[i].get_rad_mm()),active_beams[i].get_rad_mm()*2,active_beams[i].get_rad_mm()*2);
                        if(active_beams[i].get_highlighted()==false){
                            map.fill(render_shape);
                        } else {
                            if(blink_on){
                                map.fill(render_shape);
                            }
                        }
                        map.setStroke(thin_stroke);
                        map.setPaint(Color.black);
                        map.draw(render_shape);
                    }
                }
            }
            //Stored beams render
            if(show_stored_beams){
                double mag=1.2;
                if(stored_beams!=null){
                    if(stored_beams.length>0){
                        for(int i=0;i<n_stored_beams;i++){
                            if(stored_beams[i]!=null){
                                render_shape = new Ellipse2D.Double((stored_beams[i].get_xctr_mm()-mag*stored_beams[i].get_rad_mm()),(stored_beams[i].get_yctr_mm()-mag*stored_beams[i].get_rad_mm()),mag*stored_beams[i].get_rad_mm()*2,mag*stored_beams[i].get_rad_mm()*2);
                                map.setPaint(Color.red);
                                map.setStroke(thin_dashed_stroke);
                                map.draw(render_shape);
                            }
                        }
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

    @Override
    public void mouseClicked(MouseEvent e) {
        //System.out.println("Click");
        refresh();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //System.out.println("Press");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //System.out.println("Release");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //System.out.println("Enter");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //System.out.println("Exit");
    }

    void refresh() {
        repaint();
    }

    void set_beams(Circle_To_Draw[] active_beams_in, Circle_To_Draw[] stored_beams_in, boolean show_stored_beams_in) {
        active_beams = active_beams_in.clone();
        stored_beams = stored_beams_in.clone();
        n_stored_beams = stored_beams.length;
        show_stored_beams = show_stored_beams_in;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
