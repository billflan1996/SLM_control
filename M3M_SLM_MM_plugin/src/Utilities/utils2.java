/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utilities;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JOptionPane;

/**
 *
 * @author Fogim
 */
public class utils2 {
    //OHCA2 defaults
    boolean test;
    public static final String TIME_SEQ = "Time step";
    public static final String XY_SEQ = "XY position";
    public static final String Z_SEQ = "Z stack";
    public static final String CONF_SEQ = "Config change";
    public static final String HL1_ON = "<font color = 'ff0000'>";
    public static final String HL1_OFF = "</font>";
    public static final String MAIN_FOLDER = "OpenHCA2_files";
    public static final String RUNNABLE_SUBFOLDER = "OpenHCA2_runnables";
    public static final String PLATEPROPS_SUBFOLDER = "OpenHCA2_plate_properties";
    public static final String STAGE_MAPPING = "OpenHCA2_stage_mapping.txt";
    public static final String CONFIG_FILE = "OpenHCA2_config.txt";
    public static final String JSON_DELIMITER = "$#!+*";
    //STAGE POSITION PROPERTY NAME REFS
    public static final String POS_SAVE_GENERATED_BY = "POS_SAVE_GEN_BY";
    public static final String POS_SAVE_TYPE_STRIP_TILING = "Strip tiling";
    public static final String POS_SAVE_TYPE_SPIRAL = "Spiral";
    public static final String POS_SAVE_TYPE_RANDOM = "Random";
    public static final String POS_SAVE_TYPE_AREA_GRID = "Area grid";
    public static final String OHCA2_WELL = "OpenHCA2_Well";
    public static final String OHCA2_INTRA_PATTERN_INDEX = "OpenHCA2_Intra_pattern_index";
    public static final String OHCA2_INTRA_WELL_GROUPING = "OpenHCA2_Intra_well_grouping";
    //OTHER STUFF LATER
    
    public utils2(){
    }
    
    public String[] needed_folders(){
        String[] folder_list = new String[]{
            PLATEPROPS_SUBFOLDER,
            RUNNABLE_SUBFOLDER
        };
        return folder_list;
    }
    
    public void generate_missing_folders(Path IJ_path){
        if(IJ_path.resolve(MAIN_FOLDER).toFile().exists()){
            //Good
        } else {
            IJ_path.resolve(MAIN_FOLDER).toFile().mkdir();
            //ADD POPUP NOTIFICATION! Make readme too?
        }
        //Once we're sure that the main is there, check if subdirs are too...
        for (String subdir : needed_folders()){
            if(IJ_path.resolve(MAIN_FOLDER).resolve(subdir).toFile().exists()){
                //Good
            } else {
                IJ_path.resolve(MAIN_FOLDER).resolve(subdir).toFile().mkdir();
                //ADD POPUP NOTIFICATION!
            }            
        }
    }

    public double abs_min_diff_dbl(ArrayList<Double> inputlist){
        double min_diff = Double.MAX_VALUE;//Default position is to give the worst-case scenario
        if(inputlist.size()<2){
            min_diff = 0;
        } else {
            Collections.sort(inputlist);
            int i=0;
            while (i<(inputlist.size()-1)){
                double delta = inputlist.get(i+1)-inputlist.get(i);
                if(delta<Math.abs(min_diff)){
                    min_diff = delta;
                }
                i++;
            }
        }
        return min_diff;   
    }
    
    public int abs_min_diff_int(ArrayList<Integer> inputlist){
        ArrayList<Double> trans_list = new ArrayList<Double>();
        for(int val : inputlist){
            trans_list.add((double)val);
        }
        double ans = abs_min_diff_dbl(trans_list);
        return (int)ans;
    }
    
    public boolean does_file_exist(File file_to_check){
        return false;
    }
    
    public int s_to_ms(double s){
        return Integer.parseInt(read_num_sensible(String.valueOf(s*1000.0),true));
    }
    
    public int min_to_ms(double s){
        return Integer.parseInt(read_num_sensible(String.valueOf(s*1000*60),true));
    }    
    
    public int hr_to_ms(double s){
        return Integer.parseInt(read_num_sensible(String.valueOf(s*1000*60*60),true));
    }        
    
    public double ms_to_s(int ms){
        return Double.parseDouble(read_num_sensible(String.valueOf(ms/1000),false,true,3));//3d.p.s
    }
    
    public double ms_to_min(int ms){
        return Double.parseDouble(read_num_sensible(String.valueOf(ms/(60*1000)),false,true,3));//3d.p.s
    }    
    
    public double ms_to_hr(int ms){
        return Double.parseDouble(read_num_sensible(String.valueOf(ms/(60*60*1000)),false,true,3));//3d.p.s
    }        
    
    public String give_html(String inputstr, String format){
        boolean skip = false;
        format = format.toUpperCase();
        if (format == "I" || format == "B" || format =="U"){
        } else {
            skip = true;
        }
        if (skip == true){
            return(inputstr);
        } else {
            return("<"+format+">"+inputstr+"</"+format+">");
        }
    }
    
    public int option_popup(Object parent, String title, Object message, Object[] options){
        return JOptionPane.showOptionDialog((Component) parent, message,title, JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[0]);
    }
    
    //https://deano.me/2012/01/java-resize-arrays-multi-dimensional-arrays/
    public static Object resizeArray(Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(elementType,newSize);
        int preserveLength = Math.min(oldSize,newSize);
        if (preserveLength > 0) {
            System.arraycopy(oldArray,0,newArray,0,preserveLength);
        }
        return newArray;
    }

    public String strip_non_numeric(String stringtostrip){
        int negcheck = stringtostrip.indexOf("-");
        String strippedstring = stringtostrip.replaceAll("[^\\d.]", "");
        String nodots = strippedstring.replaceAll("[^\\d]","");
        int firstdot = strippedstring.indexOf(".");//First decimal point will be taken
        String floatval;
        if(firstdot>=0){
            floatval = nodots.substring(0,firstdot)+"."+nodots.substring(firstdot);
        } else {
            floatval = nodots;
        }
        if(negcheck == 0 && nodots.length()>0){//First character is a - sign, and there is actually a number there
            floatval = "-"+floatval;
        }
        return floatval;
    }
    
    public String constrain_val(String val, double min, double max){
        return Double.toString(constrain_val(Double.parseDouble(val),min,max));
    }
    
    public int set_decent_step(ArrayList<Double> scale_options, double range, int num_steps_min){
        Collections.sort(scale_options);
        int use_step = 1;
        for (double scale_opt : scale_options){
            if(range>(scale_opt*num_steps_min)){
                use_step = (int)scale_opt;
            }
        }
        return use_step;
    }
    
    public int round_to_nearest(double  value, int stepsize){
        return (int)(stepsize*(Math.round(value/stepsize)));
    }
    
    public int round_up_to_nearest(double  value, int stepsize){
        return (int)(stepsize*(Math.ceil(value/stepsize)));
    }    
    
    public int round_down_to_nearest(double value, int stepsize){
        return (int)(stepsize*(Math.floor(value/stepsize)));
    }        

    public double constrain_val(double val, double min, double max){    
        if (val<min){
            val = min;
        }
        if (val>max){
            val = max;
        }
        return val;
    }
    
    public float sum_arr(byte[] arr){
        float total = 0;
        for (int i=0;i<arr.length;i++){
            total+=arr[i];
        }
        return total;
    }
    
    public float sum_arr(short[] arr){
        float total = 0;
        for (int i=0;i<arr.length;i++){
            total+=arr[i];
        }
        return total;        
    }    
    
    public String read_num_sensible(String input_value, boolean force_int, boolean pos_only, int num_dp){
        //Probably a stupid way to get right # of decimal points, but it should at least be 'safe'...
        //https://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
        String retval = "0.0";
        if(strip_non_numeric(input_value).replaceAll("-", "").length()<1){//Also, just "-" would be bad...
            //Leave everything as zero...
        } else {
            double value = Double.parseDouble(strip_non_numeric(input_value));
            if(pos_only){
                value = Math.abs(value);
            }
            if(force_int){
                value = Math.round(value);
                return Integer.toString((int)value);
            }

            retval = Double.toString(value);
            if (num_dp>0){
                if(retval.indexOf(".")>0){
                    if(retval.length()>retval.indexOf(".")+num_dp){
                        retval = retval.substring(0, retval.indexOf(".")+num_dp+1);
                    } else {
                        retval = retval.substring(0, retval.length());
                    }
                }
            }
        }
        return retval;
    }    
    
    public String read_num_sensible(String input_value, boolean force_int, boolean pos_only){
        return read_num_sensible(input_value, force_int, pos_only, -1);
    }
    
    //Overloading for the lazy. This definitely won't lead to problems...
    public String read_num_sensible(String input_value, boolean force_int){
        return read_num_sensible(input_value, force_int, false);
    }
    public String read_num_sensible(String input_value){
        return read_num_sensible(input_value, false, false);
    }    
    
    public String force_sf(Double inputnum, Integer n_dp, Integer zero_pad_length){
        //Decimal places
        String string_val = String.format("%."+n_dp.toString()+"f", inputnum);
        //Zeropad (including the dot)
        while(string_val.length()<zero_pad_length){
            string_val = "0"+string_val;
            System.out.println(string_val);
        }
        return inputnum.toString();
    }
}
