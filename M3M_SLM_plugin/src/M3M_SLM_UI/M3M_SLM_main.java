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

import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

/**
 *
 * @author Sunil Kumar <sunil.kumar@imperial.ac.uk>
 */

@Plugin(type = MenuPlugin.class)
public class M3M_SLM_main implements MenuPlugin, SciJavaPlugin{
    public static final String menuName = "SLM control for M3M";
    private Studio gui_;
    public static M3M_SLM_hostframe frame_ = null;
    private boolean init = false;
    private boolean busy_condition = false;
    
     @Override
    public String getSubMenu() {
        return("openScopes");
    }

    @Override
    public void onPluginSelected() {
        if(frame_ == null){
            frame_ = new M3M_SLM_hostframe(gui_);
            frame_.setVisible(true);
        }
        frame_.setparent(this);
        init = true;
    }
  
    @Override
    public void setContext(Studio studio) {
        gui_ = studio;
    }
  
    @Override
    public String getName() {
        return menuName;
    }

    @Override
    public String getHelpText() {
        return("Widefield image acquisition software - see https://github.com/imperial-photonics/M3M_SLM_plugin/");
    }

    @Override
    public String getVersion() {
        return("0.1.0");
    }

    @Override
    public String getCopyright() {
        return("Copyright Imperial College London [2023]");
    }
    
    //"API" from here on

    public boolean is_busy(){
        return busy_condition;
    }
}
