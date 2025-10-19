import java.io.*;
import java.net.*;

public class HW1Client {
    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        System.out.print("Connecting to server " + hostName + ":" + portNumber + "...");
        try (
                Socket proxySocket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(proxySocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(proxySocket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(
                        new InputStreamReader(System.in))) {
            System.out.println("Socket Opened!");
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                // Stores current line from proxy.
                String line;
                // Stores length of content to read in bytes.
                int contentLength;

                // Read first line of input. This should either be the content length or an error message.
                line = in.readLine();
                
                try {
                    // Try to extract the content length from the first line of the response.
                    contentLength = Integer.parseInt(line);
                } catch (Exception e) {
                    // If it failed, then an error was encountered.
                    // I chose to have error handling done in this manner that way the connection wouldn't be severed from the server when an error occurs.
                    System.out.println(line);
                    continue;
                }

                String filename = in.readLine();
                System.out.println("---Downloading---");
                System.out.println("Content-Length: " + contentLength);
                System.out.println("Filename: " + filename);

                // FileWriter used for saving file locally.
                try (
                        FileWriter fileWriter = new FileWriter(filename);
                        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

                    char[] buffer = new char[1024]; // Buffer stores up to 1024 characters at a time.
                    while (contentLength > 0) {
                        // Read at most the length of the buffer, or less if there are fewer characters remaining.
                        int bytesToRead = Math.min(buffer.length, contentLength);
                        int bytesRead = in.read(buffer, 0, bytesToRead);

                        // If end of stream has been reached, exit instantly.
                        if (bytesRead == -1)
                            break;

                        // Save data to file and decrement contentLength.
                        bufferedWriter.write(buffer, 0, bytesRead);
                        contentLength -= bytesRead;
                    }

                    System.out.println("Done! File saved as: " + filename);
                } catch (IOException e) {
                    System.err.println("Error writing to file: " + filename);
                    System.exit(1);
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostName);
            System.exit(1);
        }
    }
}
