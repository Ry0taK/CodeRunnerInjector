package me.ryotak;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.instrument.Instrumentation;

public class InjectCodeRunnerGui {
    private static JTextField field;
    public static void agentmain(String agentArgs, Instrumentation inst) {
        JFrame frame = new JFrame("CodeRunnerInjector GUI");
        frame.setSize(200,120);
        frame.setLayout(null);
        field = new JTextField();
        field.setSize(200,30);
        JButton button = new JButton("Execute");
        button.setBounds(0,50,200,50);
        button.addActionListener(new CodeExecuteListener());
        frame.add(field);
        frame.add(button);
        frame.setVisible(true);
    }
    static class CodeExecuteListener implements ActionListener{
        public void actionPerformed(ActionEvent event) {
            if(!field.getText().isEmpty()){
                try {
                    Object out = new ScriptEngineManager().getEngineByName("Nashorn").eval(field.getText());
                    if (out == null) out = "Code successfully executed!";
                    JOptionPane.showMessageDialog(null,out);
                }catch (ScriptException e){
                    JOptionPane.showMessageDialog(null,e);
                }
            }
        }
    }
}
