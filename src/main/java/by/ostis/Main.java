package by.ostis;

import by.ostis.client.WebSocketClient;
import org.apache.commons.cli.*;

import javax.websocket.ClientEndpoint;
import java.io.File;

@ClientEndpoint
public class Main {
    private static String OUTPUT_DIRECTORY = "agents";


    public static void main(String[] args) throws InterruptedException {
        Options options = new Options();
        options.addOption("h", "help", false, "display this help message");
        options.addOption("o", "output-directory", true, "directory to put translated agents into, default is " + OUTPUT_DIRECTORY);
        options.addOption("r", "root", true, "this field is passed automatically, it is equal to root of this project");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                helpAndExit(options);
            }
            try {
                String root = "";
                if (cmd.hasOption("r")) {
                    root = cmd.getOptionValue("r") + File.separator;
                }
                if (cmd.hasOption("o")) {
                    OUTPUT_DIRECTORY = cmd.getOptionValue("o");
                }
                OUTPUT_DIRECTORY = root + OUTPUT_DIRECTORY;
            } catch (NumberFormatException exception) {
            }
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
        WebSocketClient webSocketClient = new WebSocketClient(OUTPUT_DIRECTORY);
        webSocketClient.start();
        synchronized (webSocketClient) {
            webSocketClient.wait();
        }
    }

    private static void helpAndExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("start.sh [args]", "sc-server should be running", options, "\n--------------------------\nauthor: kilativ-dotcom");
        System.exit(0);
    }
}