/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.net.*;
import java.io.*;

public class HW1Server {
    public static void main(String[] args) throws IOException {
        
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        
        int portNumber = Integer.parseInt(args[0]);
        
        try{
            ServerSocket serverSocket =
                new ServerSocket(portNumber);

            while(true){
                Socket clientSocket = serverSocket.accept();
                
                
                ClientWorker w=new ClientWorker(clientSocket);
                Thread t=new Thread(w);
                t.start();
            }
        
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}

class ClientWorker implements Runnable {
  private Socket client;

  //Constructor
  ClientWorker(Socket client) {
    this.client = client;
  }

  public void run(){
    String line;
    BufferedReader in = null;
    PrintWriter out = null;
    try{
      in = new BufferedReader(new 
        InputStreamReader(client.getInputStream()));
      out = new 
        PrintWriter(client.getOutputStream(), true);
    } catch (IOException e) {
      System.out.println("in or out failed");
      System.exit(-1);
    }

    while(true){
      try {
        line = in.readLine();
        if (line == null) {
          throw new IllegalArgumentException("Request was empty!");
        }

        // Incoming request should be: GET example.com/index.html or GET example.com
        String[] request = line.split(" ");

        // Error checking user input. Continues to next request when error is found.
        if (request.length != 2) {
          throw new IllegalArgumentException("Requests must be in format: GET <url>");
        } else if (!request[0].equals("GET")) {
          throw new IllegalArgumentException("Only GET requests are supported.");
        }

        String[] url = request[1].split("/", 2);

        proxyConnection(url, out);
       } catch (IOException e) {
        System.out.println("Read failed");
        System.exit(-1);
       } catch (IllegalArgumentException e) {
        out.println("Error: " + e);
       }
    }
  }

  private void proxyConnection(String[] url, PrintWriter clientOut) {
    if (url.length == 0) {
      clientOut.println("Invalid url provided");
      return;
    }

    String hostName = url[0];
    String file = "";

    // If only hostname was given, assume file is /index.html
    if (url.length == 1) {
      file = "/index.html";
    } else {
      file = "/" + url[1];
    }

     try (
      // Port 80 is hardcoded for HTTP requests.
      Socket proxyClientSocket = new Socket(hostName, 80);
      PrintWriter out =
        new PrintWriter(proxyClientSocket.getOutputStream());
      BufferedReader in = 
        new BufferedReader(
          new InputStreamReader(proxyClientSocket.getInputStream()));
    ) {
      // Build HTTP Request from user's input.
      String request = "GET " + file + " HTTP/1.1\r\nHost: " + hostName + "\r\n\r\n";
      // Write it out and then flush the buffer.
      out.print(request);
      out.flush();

      String line;
      while ((line = in.readLine()) != null) {
          System.out.print(line);
          clientOut.print(line);
          clientOut.flush();
      }
    } catch (UnknownHostException e) {
      clientOut.println("Unable to find host " + hostName);
    } catch (IOException e) {
      clientOut.println("Couldn't get I/O for the connection to " + hostName);
    }
  }
}
