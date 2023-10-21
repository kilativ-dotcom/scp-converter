package by.ostis;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    private static final long INITIAL_SLEEP = 1000;
    private static final long PRESS_PRESS_SLEEP = 40;
    private static final long PRESS_RELEASE_SLEEP = 20;
    private static final long OPEN_PASTE_SLEEP = 150;
    private static final long BEFORE_ENTER_SLEEP = 100;
    private static final long LOAD_FILE_SLEEP = 150;
    private static final long AFTER_FILE_SLEEP = 50;
    public static final Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.out.println("something went wrong, cannot start application");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Set<String> gwfFiles = Arrays.stream(args)
                .map(File::new)
                .filter(File::exists)
                .map(File::toPath)
                .flatMap(path -> {
                    try {
                        return Files.walk(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Path::toFile)
                .map(File::getAbsoluteFile)
                .map(File::toString)
                .filter(file -> file.endsWith(".gwf"))
                .collect(Collectors.toSet());
        resaveFiles(gwfFiles);
        System.out.println("Hello world!");
    }

    private static void resaveFiles(Set<String> gwfFiles) {
        System.out.println("about to convert " + gwfFiles.size() + " file(s) " + gwfFiles);
        sleep(INITIAL_SLEEP);


        System.out.println("start = " + System.currentTimeMillis());
        for (String file : gwfFiles) {
            resaveFile(file);
            sleep(AFTER_FILE_SLEEP);
        }
        System.out.println("end = " + System.currentTimeMillis());
    }

    private static void resaveFile(String file) {
        System.out.println("converting " + file);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(file), null);
        pressButtons(KeyEvent.VK_CONTROL, KeyEvent.VK_O);
        sleep(OPEN_PASTE_SLEEP);
        pressButtons(KeyEvent.VK_CONTROL, KeyEvent.VK_V);
        sleep(BEFORE_ENTER_SLEEP);
        pressButtons(KeyEvent.VK_ENTER);
        sleep(LOAD_FILE_SLEEP);
        pressButtons(KeyEvent.VK_ESCAPE);
        pressButtons(KeyEvent.VK_ESCAPE);
        pressButtons(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
        pressButtons(KeyEvent.VK_CONTROL, KeyEvent.VK_LEFT);
        pressButtons(KeyEvent.VK_CONTROL, KeyEvent.VK_RIGHT);
        pressButtons(KeyEvent.VK_CONTROL, KeyEvent.VK_S);
    }

    private static void pressButtons(int... buttons) {
        for (int button : buttons) {
            Main.robot.keyPress(button);
            sleep(PRESS_PRESS_SLEEP);
        }
        sleep(PRESS_RELEASE_SLEEP);
        for (int button : buttons) {
            Main.robot.keyRelease(button);
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}