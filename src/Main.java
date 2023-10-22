import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    private static final String contentGroupName = "filename";
    private static final Pattern scsiPattern = Pattern.compile("\\Q[*^\"\\Efile://(?<" + contentGroupName + ">.*?)\\Q\"*]\\E");
    private static final Set<String> movedFiles = new HashSet<>();

    public static void main(String[] args) {
        Set<String> scsiFiles = getFilesWithExtention(args, ".scsi");
        System.out.println("scsiFiles = " + scsiFiles.toString());
        Map<String, Long> scsiNames = scsiFiles.stream().map(File::new).map(File::getName).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        System.out.println("scsiFiles.size() = " + scsiFiles.size());
        System.out.println("scsiNames.size() = " + scsiNames.size());
        for (Map.Entry<String, Long> nameCount : scsiNames.entrySet()) {
            if (nameCount.getValue() != 1) {
                System.out.println("\n    Attention!!!! found multiple occurrences for " + nameCount);
            }
        }
        Set<String> scsFiles = getFilesWithExtention(args, ".scs");
        Set<String> filesWithScsiMention = scsFiles.stream().filter(name -> {
            try {
                return Files.readAllLines((new File(name)).toPath()).stream().anyMatch(line -> scsiPattern.matcher(line).find());
            } catch (IOException fnf) {
                System.out.println("fnf.getMessage() = " + fnf.getMessage());
                return false;
            }
        }).collect(Collectors.toSet());
        System.out.println("filesWithScsiMention.size() = " + filesWithScsiMention.size());
        scsiFiles.addAll(scsFiles);
        filesWithScsiMention.stream().map(File::new).map(File::toPath).forEach(f -> Main.processFile(f, scsiFiles));
        movedFiles.stream().forEach(file -> {
            try {
                System.out.println("renaming " + file);
                Files.move(new File(file).toPath(), new File(file + "-inserted").toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Hello world!");
    }

    private static void processFile(Path path, Set<String> scsiFiles) {
        System.out.print("Processing " + path);
        try {
            String entiretyOfAFile = String.join("\n", Files.readAllLines(path));
            Matcher matcher = scsiPattern.matcher(entiretyOfAFile);
            boolean foundAtLeastOne = false;
            while (matcher.find()) {
                String partName = matcher.group(contentGroupName);
                if (partName == null || partName.isEmpty()) {
                    continue;
                }
                List<String> candidateFiles = scsiFiles.stream().filter(fullName -> fullName.endsWith("/" + partName)).collect(Collectors.toList());
                if (candidateFiles.size() != 1) {
                    System.out.println("\n  found more than one candidate for scsi " + partName + ".\n\t  to be exact " + candidateFiles);
                    return;
                }
                String candidateFile = candidateFiles.get(0);
                movedFiles.add(candidateFile);
                String entiretyOfACandidateFile = String.join("\n", Files.readAllLines(new File(candidateFile).toPath()));
                entiretyOfAFile = entiretyOfAFile.replace(matcher.group(0), "[*\n" + entiretyOfACandidateFile + "\n*]");
                foundAtLeastOne = true;
            }
            Files.write(path, entiretyOfAFile.getBytes());
            if (foundAtLeastOne) {
                System.out.println(" --- [modified]");
            } else {
                System.out.println();
            }
        } catch (IOException e) {
            System.out.println("\nError: " + path + " : " + e.getMessage());
        }
    }

    private static Set<String> getFilesWithExtention(String[] args, String suffix) {
        return Arrays.stream(args)
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
                .filter(file -> file.endsWith(suffix))
                .collect(Collectors.toSet());
    }
}