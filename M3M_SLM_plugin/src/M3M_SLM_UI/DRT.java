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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kumars2
 */
public class DRT implements Runnable{
    private M3M_SLM_hostframe parent_ = null;
    boolean quit_loop = false;
    boolean running = false;
    
    public DRT (M3M_SLM_hostframe parent_ref){
        parent_ = parent_ref;
    }    
    
    @Override
    public void run() {
        running = true;
        while (quit_loop == false){
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                
            }
            if(parent_.get_drawing_paused() == false){
                //System.out.println("Graphics update");
                parent_.refresh_panels();
            }
        }
        running = false;
    }
    
}
