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

import java.awt.Color;

/**
 *
 * @author Sunil
 */
public class Circle_To_Draw {
    private boolean fill = false;
    private double radius_mm;
    private double x_ctr_mm;
    private double y_ctr_mm;
    private String name = null;
    private Color beam_colour = Color.red;
    private boolean highlighted = false;
    
    void set_fill(boolean newval){
        fill = newval;
    }
    boolean get_fill(){
        return fill;
    }
    
    void set_highlighted(boolean highlight){
        highlighted = highlight;
    }
    boolean get_highlighted(){
        return highlighted;
    }
    
    void set_colour(Color newcol){
        beam_colour = newcol;
    }
    Color get_colour(){
        return beam_colour;
    }
    
    void set_rad_mm(double newval){
        radius_mm = newval;
    }
    double get_rad_mm(){
        return radius_mm;
    }
    
    void set_xctr_mm(double newval){
        x_ctr_mm = newval;
    }
    double get_xctr_mm(){
        return x_ctr_mm;
    }
    
    void set_yctr_mm(double newval){
        y_ctr_mm = newval;
    }
    double get_yctr_mm(){
        return y_ctr_mm;
    }
    
    void set_name(String newval){
        name = newval;
    }
    String get_name(){
        return name;
    }    
    
    public Circle_To_Draw(){
        fill = false;
        radius_mm = 0;
        x_ctr_mm = 0;
        y_ctr_mm = 0;
        name = null;
        beam_colour = null;
        highlighted = false;
    }
    
    public Circle_To_Draw(String name_in, boolean fill_in, double radius_mm_in, double xctr_mm_in, double yctr_mm_in, Color beam_colour_in, boolean highlighted_in){
        name = name_in;
        fill = fill_in;
        radius_mm = radius_mm_in;
        x_ctr_mm = xctr_mm_in;
        y_ctr_mm = yctr_mm_in;
        beam_colour = beam_colour_in;
        highlighted = highlighted_in;
    }
     
    public Circle_To_Draw(Circle_To_Draw input_circ){
        name = input_circ.get_name();
        fill = input_circ.get_fill();
        radius_mm = input_circ.get_rad_mm();
        x_ctr_mm = input_circ.get_xctr_mm();
        y_ctr_mm = input_circ.get_yctr_mm();
        beam_colour = input_circ.get_colour();
        highlighted = input_circ.get_highlighted();
    }
}

