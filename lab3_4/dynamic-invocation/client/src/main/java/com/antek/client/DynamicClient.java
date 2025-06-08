package com.antek.client;

import com.zeroc.Ice.*;
import com.zeroc.Ice.Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Exception;
import java.util.Objects;

public class DynamicClient {
    public static void main(String[] args) {
        int status = 0;
        Communicator communicator = null;

        try {
            communicator = Util.initialize(args);

            ObjectPrx base = communicator.stringToProxy("calc/calc:tcp -h 127.0.0.2 -p 10000 -z : udp -h 127.0.0.2 -p 10000 -z"); //opcja -z włącza możliwość kompresji wiadomości

            String line = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            A a;

            do {
                try {
                    System.out.print("==> ");
                    line = in.readLine();
                    switch (line) {
                        case "add":
                            dynamicAdd(base,7,8);
                            break;
                        case "subtract":
                            dynamicSubtract(base,7,8);
                            break;
                        case "op":
                            dynamicOp(base, (short) 11,22,33.0f,"ala ma kota",(short) 44);
                            break;
                        default:
                            System.out.println("???");
                    }
                } catch (IOException | TwowayOnlyException ex) {
                    ex.printStackTrace(System.err);
                }
            }
            while (!Objects.equals(line, "x"));


        } catch (LocalException e) {
            e.printStackTrace();
            status = 1;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            status = 1;
        }
        if (communicator != null) { //clean
            try {
                communicator.destroy();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                status = 1;
            }
        }
        System.exit(status);
    }


    private static void dynamicAdd(ObjectPrx base, int a, int b){
        Communicator communicator = base.ice_getCommunicator();
        try{
            OutputStream out = new OutputStream(communicator);
            out.startEncapsulation();
            out.writeInt(a);
            out.writeInt(b);
            out.endEncapsulation();
            byte[] inParams = out.finished();

            Object.Ice_invokeResult r = base.ice_invoke("add", OperationMode.Normal, inParams);
            if(r.returnValue){
                InputStream in = new InputStream(communicator, r.outParams);
                in.startEncapsulation();
                long result = in.readLong();
                in.endEncapsulation();
                System.out.println("Result: " + result);
            }else{
                System.out.println("Error while adding");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void dynamicSubtract(ObjectPrx base, int a, int b){
        Communicator communicator = base.ice_getCommunicator();
        try{
            OutputStream out = new OutputStream(communicator);
            out.startEncapsulation();
            out.writeInt(a);
            out.writeInt(b);
            out.endEncapsulation();

            byte[] inParams = out.finished();

            Object.Ice_invokeResult r = base.ice_invoke("subtract",OperationMode.Normal, inParams);
            if(r.returnValue){
                InputStream in = new InputStream(communicator, r.outParams);
                in.startEncapsulation();
                long result = in.readLong();
                in.endEncapsulation();
                System.out.println("Result: " + result);

            }else{
                System.out.println("Error while adding");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void dynamicOp(ObjectPrx base, short a, long b, float c, String d, short b1){
        Communicator communicator = base.ice_getCommunicator();
        try{
            OutputStream out = new OutputStream(communicator);
            out.startEncapsulation();
            out.writeShort(a);
            out.writeLong(b);
            out.writeFloat(c);
            out.writeString(d);

            out.writeShort(b1);
            out.endEncapsulation();

            byte[] inParams = out.finished();
            Object.Ice_invokeResult r = base.ice_invoke("op",OperationMode.Normal, inParams);
            if(r.returnValue){
                InputStream in = new InputStream(communicator, r.outParams);
                in.startEncapsulation();
                in.endEncapsulation();
                System.out.println("DONE");
            }else{
                System.out.println("NOT DONE");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}