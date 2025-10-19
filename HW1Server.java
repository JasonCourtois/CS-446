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

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);

            System.out.println("Server started, listening on port: " + portNumber);
            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientWorker w = new ClientWorker(clientSocket);
                Thread t = new Thread(w);
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

    // Constructor
    ClientWorker(Socket client) {
        this.client = client;
    }

    public void run() {
        String line;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("in or out failed");
            System.exit(-1);
        }

        // Output client's IP address on connection.
        System.out.println("Connection made from: " + client.getRemoteSocketAddress());

        // Runs forever while client is connected in order to receive GET requests.
        while (true) {
            try {
                line = in.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Request was empty!");
                }

                // Incoming request should be: GET example.com/index.html or GET example.com
                String[] request = line.split(" ");

                // Ensures requests are made in the correct format of: GET <url>
                if (request.length != 2) {
                    throw new IllegalArgumentException("Requests must be in format: GET <url>");
                } else if (!request[0].equals("GET")) {
                    throw new IllegalArgumentException("Only GET requests are supported.");
                }

                // Separates the hostname from the rest of the path given in URL by splitting at first / character.
                String[] url = request[1].split("/", 2);

                // proxyConnection function handles contacting remote webserver and transmitting back to client.
                // I chose to create a new function to handle this extra connection to not make the the run function cluttered.
                proxyConnection(url, out);
            } catch (IOException e) {
                System.out.println("Read failed");
                System.exit(-1);
            } catch (IllegalArgumentException e) {
                // User input errors are sent back to client.
                out.println("Error: " + e);
            }
        }
    }

    // Accepts an array of strings for the url passed in by user, and a reference to the clientOut print writer to send data back to user.
    private void proxyConnection(String[] url, PrintWriter clientOut) {
        if (url.length == 0) {
            throw new IllegalArgumentException("Invalid url provided");
        }

        // Extract hostname and path from the url array.
        String hostName = url[0];
        String path = "/";

        // If url length is 2, a path was provided by user. Add it to existing path.
        if (url.length == 2) {
            path = path + url[1];
        }

        // Create a socket connection to remote webserver.
        try (
                // Port 80 is hardcoded for HTTP requests.
                Socket proxyClientSocket = new Socket(hostName, 80);
                PrintWriter out = new PrintWriter(proxyClientSocket.getOutputStream());
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(proxyClientSocket.getInputStream()));) {
            // Build HTTP Request from user's input.
            String request = "GET " + path + " HTTP/1.1\r\nHost: " + hostName + "\r\n\r\n";
            // Write out the request and then flush the buffer.
            out.print(request);
            out.flush();

            String line;
            int contentLength = -1;
            // Skips headers of HTTP response, but saves Content-Length if found.
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length")) {
                    String[] headerParts = line.split(": ");

                    contentLength = Integer.parseInt(headerParts[1]);
                }
            }

            // contentLength will only ever be -1 if it wasn't found in HTTP headers.
            if (contentLength == -1) {
                throw new InternalError("Unable to find Content-Length header");
            }

            // Send content length from header.
            clientOut.println(contentLength);

            // Send file name back to client. Get filename by splitting path at / character.
            String[] folders = path.split("/");
            if (folders.length > 0 && folders[folders.length - 1].endsWith(".html")) {
                // If a path was given, and last item ends in .html use that for the file name.
                clientOut.println(folders[folders.length - 1]);
            } else {
                // Otherwise file will be saved as index.html.
                clientOut.println("index.html");
            }

            char[] buffer = new char[1024];
            // contentLength is decremented as bytes are read.
            while (contentLength > 0) {
                // Read at most the length of the buffer, or less if there are fewer characters remaining.
                int bytesToRead = Math.min(buffer.length, contentLength);
                int bytesRead = in.read(buffer, 0, bytesToRead);

                // If end of stream has been reached, exit.
                if (bytesRead == -1)
                    break;

                // Print data to clientOut and decrement contentLength
                clientOut.print(new String(buffer, 0, bytesRead));
                contentLength -= bytesRead;
            }

            clientOut.flush();
        } catch (UnknownHostException e) {
            clientOut.println("Unable to find host " + hostName);
        } catch (IOException e) {
            clientOut.println("Couldn't get I/O for the connection to " + hostName);
        } catch (InternalError e) {
            clientOut.println("Error: " + e);
        }
    }
}
