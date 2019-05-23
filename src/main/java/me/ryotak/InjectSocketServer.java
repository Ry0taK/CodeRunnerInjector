package me.ryotak;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class InjectSocketServer {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        try{

            Socket passCodeSocket = new Socket();
            passCodeSocket.connect(new InetSocketAddress(11586));
            BufferedReader passCodeReader = new BufferedReader(new InputStreamReader(passCodeSocket.getInputStream()));
            PrintWriter passCodeWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(passCodeSocket.getOutputStream())));
            passCodeWriter.println("Hello");
            passCodeWriter.flush();
            String passcode = passCodeReader.readLine();
            if(!passcode.startsWith(Values.PASSCODE_PREFIX)){
                passCodeSocket.close();
                return;
            }
            passcode = passcode.replaceFirst(Values.PASSCODE_PREFIX,"");
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(null);
            passCodeWriter.println(serverSocket.getLocalPort());
            passCodeWriter.flush();
            passCodeWriter.close();
            Socket socket = serverSocket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            while(true){
                try {
                    String message = reader.readLine();
                    if(message == null){
                        System.out.println("Host process got killed.");
                        break;
                    }
                    if (message.startsWith(Values.PASSCODE_PREFIX) && message.contains(Values.PASSCODE_SUFFIX)) {
                        message = message.replace(Values.PASSCODE_PREFIX, "");
                        String[] messages = message.split(Values.PASSCODE_SUFFIX);
                        String pass = messages[0];
                        if(messages.length > 1) {
                            if (pass != null) {
                                if (pass.equals(passcode)) {
                                    String code = messages[1];
                                    try {
                                        Object out = new ScriptEngineManager().getEngineByName("Nashorn").eval(code);
                                        if (out == null) out = "success";
                                        writer.println(out);
                                        writer.flush();
                                    } catch (ScriptException e) {
                                        writer.println("ERROR " + e);
                                        writer.flush();
                                    }
                                }
                            }
                        }
                    }
                }catch (SocketException e){
                    if(e.getMessage().equals("Connection reset")){
                        System.out.println("Host process got killed.");
                        break;
                    }
                    e.printStackTrace();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
