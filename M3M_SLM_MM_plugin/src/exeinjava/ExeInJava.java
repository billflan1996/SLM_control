/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package exeinjava;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author h.liu
 */
public class ExeInJava {

    boolean isCMDOn = false;
    ProcessBuilder builder = new ProcessBuilder();
    Process process;

    BufferedReader reader;
    BufferedWriter writer;
    BufferedReader error;

    ExecutorService exe_reader;
    ExecutorService exe_writer;
    ExecutorService exe_error;

    ArrayList<String> Readers;
    ArrayList<String> Errors;

    public void startCMD() {
        if (isCMDOn) {
            stopCMD(100);
        }

        exe_reader = Executors.newSingleThreadExecutor();
        exe_writer = Executors.newSingleThreadExecutor();
        exe_error = Executors.newSingleThreadExecutor();
        Readers = new ArrayList<String>();
        Errors = new ArrayList<String>();

        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
            String[] cmds = {"cmd.exe"};
            builder.command(cmds);
        } else {
            String[] cmds = {"/bin/sh"};
            builder.command(cmds);
        }

        builder.directory(Paths.get(System.getProperty("user.home")).toFile());
        builder.redirectErrorStream(false);
        try {
            process = builder.start();
            isCMDOn = true;

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        } catch (IOException ex) {
            Logger.getLogger(ExeInJava.class.getName()).log(Level.SEVERE, null, ex);
        }

        exe_reader.submit(() -> {
            String read = null;
            try {
                while (!Thread.currentThread().isInterrupted() && isCMDOn && process != null && process.isAlive()) {
                    if (reader != null && reader.ready() && (read = reader.readLine()) != null) {
                        Readers.add(read);
//                        System.out.println(read);
                    }
                }
                reader.close();
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        });

        exe_error.submit(() -> {
            String err = null;
            try {
                while (!Thread.currentThread().isInterrupted() && isCMDOn && process != null && process.isAlive()) {
                    if (error != null && error.ready() && (err = error.readLine()) != null) {
                        Errors.add(err);
//                        System.out.println(err);
                    }
                }
                error.close();
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        });
    }

    public String GetLatestOutput() {
        return Readers.isEmpty() ? null : Readers.get(Readers.size() - 1);
    }

    public void WriteCommands(String cmds) {
        exe_writer.submit(() -> {
            if (isCMDOn && process != null && process.isAlive() && writer != null) {
                try {
                    writer.write(cmds);
                    writer.newLine();
                    writer.flush();
                } catch (IOException ex) {
//                    ex.printStackTrace();
                }
            }
        });
    }

    public void stopCMD(long timeoutMS) {
        if (isCMDOn) {
            isCMDOn = false;

            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (error != null) {
                    error.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ExeInJava.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (exe_reader != null) {
                exe_reader.shutdown();
                long currenttime = System.currentTimeMillis();
                while (exe_reader != null && !exe_reader.isShutdown()) {
                    if (System.currentTimeMillis() - currenttime > timeoutMS) {
                        break;
                    }
                }
            }
            if (exe_writer != null) {
                exe_writer.shutdown();
                long currenttime = System.currentTimeMillis();
                while (exe_writer != null && !exe_writer.isShutdown()) {
                    if (System.currentTimeMillis() - currenttime > timeoutMS) {
                        break;
                    }
                }
            }
            if (exe_error != null) {
                exe_error.shutdown();
                long currenttime = System.currentTimeMillis();
                while (exe_error != null && !exe_error.isShutdown()) {
                    if (System.currentTimeMillis() - currenttime > timeoutMS) {
                        break;
                    }
                }
            }

            try {
                if (process != null) {
                    process.destroy();
                    process.waitFor(timeoutMS, TimeUnit.MILLISECONDS);

                    isCMDOn = process.isAlive();
                } else {
                    isCMDOn = false;
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(ExeInJava.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (process != null) {
                System.out.println("Is process off? " + String.valueOf(!process.isAlive()));
            }
            if (exe_reader != null) {
                System.out.println("Is reader thread off? " + String.valueOf(exe_reader.isShutdown()));
            }
            if (exe_writer != null) {
                System.out.println("Is writer thread off? " + String.valueOf(exe_writer.isShutdown()));
            }
            if (exe_error != null) {
                System.out.println("Is error thread off? " + String.valueOf(exe_error.isShutdown()));
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here
        ExeInJava cmds = new ExeInJava();
        cmds.startCMD();

        cmds.WriteCommands("cd "+"the path to the .py file"); // need modification here !!!

        cmds.WriteCommands("python3 echoInput.py");

        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null && str.startsWith("this is on")) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                Thread.sleep(100);
            }
        }

        cmds.WriteCommands("test1");
        cmds.WriteCommands("test2");
        cmds.WriteCommands("close");

        while (true) {
            String str = cmds.GetLatestOutput();
            if (str != null && str.startsWith("this is off")) {
                System.out.println(cmds.GetLatestOutput());
                break;
            } else {
                Thread.sleep(100);
            }

        }

        cmds.stopCMD(100);

        
        cmds.startCMD();
        cmds.WriteCommands("ls");
        
        Thread.sleep(1000);
        System.out.println(cmds.GetLatestOutput());
        
        cmds.stopCMD(100);
    }
}
