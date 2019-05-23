package me.ryotak;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.apache.commons.lang.RandomStringUtils;
import tk.ivybits.agent.AgentLoader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class CodeRunnerInjector {
    public static void main(String[] args){
        String type = null;
        List<VirtualMachineDescriptor> vmList = VirtualMachine.list();
        VirtualMachineDescriptor targetVM = null;
        for(int i = 0;i < args.length;i++){
            try {
                switch (args[i].toLowerCase()) {
                    case Values.PROCESS_LIST:
                        System.out.println("Running JVM list:");
                        for(VirtualMachineDescriptor vm : vmList){
                            System.out.println(vm.id()+": "+vm.displayName());
                        }
                        return;
                    case Values.TYPE:
                        switch (args[i+1]){
                            case Values.TYPE_GUI:
                                type = Values.TYPE_GUI;
                                break;
                            case Values.TYPE_SOCKET:
                                type = Values.TYPE_SOCKET;
                                break;
                            default:
                                System.err.println("Invalid type provided in -t.");
                                return;
                        }
                        break;
                    case Values.PID:
                        String pid = args[i+1];
                        for(VirtualMachineDescriptor vm : vmList){
                            if(vm.id().equals(pid)){
                                targetVM = vm;
                            }
                        }
                        if(targetVM == null){
                            System.err.println("Invalid pid provided in -p.");
                            return;
                        }
                        break;
                }
            }catch (IndexOutOfBoundsException e){
                System.err.println("No value provided in "+args[i]);
                return;
            }
        }
        if(targetVM == null){
            System.err.println("You must provide option: -p");
            return;
        }
        if(type == null){
            System.err.println("You must provide option: -t");
            return;
        }
        try {
            switch (type) {
                case Values.TYPE_SOCKET:
                    new Thread(() -> {
                        try {
                            ServerSocket serverSocket = new ServerSocket();
                            serverSocket.bind(new InetSocketAddress(11586));
                            Socket passcodeSocket = serverSocket.accept();
                            BufferedReader passCodeReader = new BufferedReader(new InputStreamReader(passcodeSocket.getInputStream()));
                            PrintWriter passCodeWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(passcodeSocket.getOutputStream())));
                            String firstMessage = passCodeReader.readLine();
                            if(!firstMessage.equals("Hello")){
                                passcodeSocket.close();
                            }
                            try{
                                String passcode = RandomStringUtils.randomAlphanumeric(16);
                                passCodeWriter.println(Values.PASSCODE_PREFIX+passcode);
                                passCodeWriter.flush();
                                int port = Integer.parseInt(passCodeReader.readLine());
                                passcodeSocket.close();
                                Socket socket = new Socket();
                                socket.connect(new InetSocketAddress(port));
                                System.out.println("Connected!");
                                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                                new Thread(()->{
                                    while(true){
                                        try {
                                            String message = reader.readLine();
                                            if(message.equals("success")){
                                                System.out.println("Code successfully executed!");
                                                continue;
                                            }else if(message.startsWith("ERROR")){
                                                System.err.println(message.replace("ERROR ",""));
                                                continue;
                                            }
                                            System.out.println(message);
                                        }catch (IOException e){
                                            if(e.getMessage().equals("Premature EOF")){
                                                System.out.println("Target process got killed.");
                                                System.exit(0);
                                            }
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                                while(true){
                                    Scanner scanner = new Scanner(System.in);
                                    String cmd = scanner.nextLine();
                                    switch (cmd.split(" ")[0]){
                                        case Values.QUIT:
                                            System.exit(0);
                                            break;
                                        default:
                                            writer.println(Values.PASSCODE_PREFIX+passcode+Values.PASSCODE_SUFFIX+cmd);
                                            writer.flush();
                                            break;
                                    }
                                }
                            }catch (NumberFormatException e){
                                passcodeSocket.close();
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }).start();
                    AgentLoader.attachAgentToJVM(targetVM.id(), InjectSocketServer.class,Values.class);
                    break;
                case Values.TYPE_GUI:
                    AgentLoader.attachAgentToJVM(targetVM.id(), InjectCodeRunnerGui.class,InjectCodeRunnerGui.CodeExecuteListener.class);
                    System.out.println("CodeRunner successfully injected!");
                    System.exit(0);
                    return;
            }
        }catch (IOException e) {
            if(e.getMessage().equals("Premature EOF")){
                System.out.println("Target process got killed.");
                System.exit(0);
            }
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
