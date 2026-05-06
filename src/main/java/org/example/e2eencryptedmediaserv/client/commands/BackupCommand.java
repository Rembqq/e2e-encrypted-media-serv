package org.example.e2eencryptedmediaserv.client.commands;


import okhttp3.*;
import org.example.e2eencryptedmediaserv.client.crypto.EncryptionService;
import org.example.e2eencryptedmediaserv.client.model.UploadMetadata;
import org.example.e2eencryptedmediaserv.client.utils.CliUtils;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotCreateRequest;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotFileRequest;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "backup",
        description = "Create encrypted backup of a directory"
)

public class BackupCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Directory to backup")
    private Path sourceDir;

    @CommandLine.Option(names = {"--server", "-s"}, required = true)
    private String serverUrl;

    @CommandLine.Option(names = {"--key", "-k"}, required = true)
    private String keyBase64;

    @CommandLine.Option(names = {"--token", "-t"}, defaultValue = "dummy-token")
    private String token;

    @CommandLine.Option(names = "--name", description = "Snapshot name", defaultValue = "auto")
    private String snapshotName;

    @CommandLine.Option(names = "--dry-run", description = "Show actions without uploading")
    private boolean dryRun;

    @CommandLine.Option(names = "--exclude", description = "Exclude patterns (glob)", split = ",", arity = "0..*")
    private List<String> excludePatterns = new ArrayList<>();

    private List<Pattern> excludeRegexes;

    @Override
    public Integer call() throws Exception {
        if(!Files.isDirectory(sourceDir)) {
            System.err.println("Not a directory: " + sourceDir);
            return 1;
        }

        if ("auto".equals(snapshotName)) {
            snapshotName = "backup-" + System.currentTimeMillis();
        }

        excludeRegexes = CliUtils.prepareExcludePatterns(excludePatterns);

        EncryptionService crypto = new EncryptionService(keyBase64);

        long totalFiles = CliUtils.countRegularFiles(sourceDir, excludeRegexes);
        System.out.printf("Found %,d regular files%n", totalFiles);
        List<SnapshotFileRequest> snapshotFiles = new ArrayList<>();
        long processed = 0;

        System.out.println("Starting backup: " + snapshotName);
        System.out.println("Source: " + sourceDir.toAbsolutePath());

        try(var walker = Files.walk(sourceDir)) {
            List<Path> files = walker
                    .filter(Files::isRegularFile)
                    .filter(path -> CliUtils.shouldIncludeFile(
                            sourceDir, path, excludeRegexes))
                    .toList();

            for (Path path : files) {
                processed++;
                String relative = sourceDir.relativize(path).toString();
                long size = Files.size(path);
                Instant modifiedAt = Files.getLastModifiedTime(path).toInstant();

                System.out.printf("\rProgress: %d / %d %-50s", processed, totalFiles, relative);

                if (dryRun) continue;

                byte[] plaintext;

                plaintext = Files.readAllBytes(path);
                var encData = crypto.encryptForBackup(plaintext);

                UploadMetadata metadata = new UploadMetadata(
                        "backup-cli",
                        relative,
                        size,
                        modifiedAt,
                        encData.blobHash()
                );

                String blobId = CliUtils.uploadBlob(
                        serverUrl, token, encData.blob(), relative, metadata
                );

                if (blobId == null) {
                    System.err.printf("%nUpload failed: %s%n", relative);
                    continue;
                }

                snapshotFiles.add(new SnapshotFileRequest(
                        relative,
                        blobId,
                        size,
                        modifiedAt
                ));
            }
        }

        System.out.println();

        if (dryRun) {
            System.out.println("Dry run completed.");
            return 0;
        }

        if (snapshotFiles.isEmpty()) {
            System.out.println("No files processed.");
            return 0;
        }

        SnapshotCreateRequest createReq = new SnapshotCreateRequest(
                snapshotName,
                "desc-1",
                snapshotFiles
        );

        String json;
        try {
            json = CliUtils.MAPPER.writeValueAsString(createReq);
        } catch (Exception e) {
            System.err.println("Cannot serialize SnapshotCreateRequest: " + e.getMessage());
            return 1;
        }

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url(serverUrl + "/api/v1/snapshots")
                .post(body)
                .addHeader("Authorization", "Bearer" + token)
                .build();

        String snapshotId = null;
        String serverResponseBody = null;

        try(Response resp = CliUtils.HTTP_CLIENT.newCall(req).execute()) {

            serverResponseBody = resp.body() != null ? resp.body().string() : "";
            if(!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "[no body]";
                System.err.println("POST /snapshots failed: " + resp.code() + " " + resp.message());
                System.err.println("Response: " + err);
                return 1;
            }

            String respBody = resp.body().string();
            System.out.println("Snapshot created");
            System.out.println("Server response: " + respBody);

            if(serverResponseBody.contains("\"id\":")) {
                snapshotId = serverResponseBody.split("\"id\":")[1].split("[,}\\s]")[0].trim();
                System.out.println("Created snapshot ID: " + snapshotId);
            } else {
                System.out.println("Warning: Could not parse snapshot ID from response");
            }

        }

        System.out.println("──────────────────────────────────────");
        System.out.printf("Backup completed successfully%n");
        System.out.printf("Snapshot name : %s%n", snapshotName);
        if (snapshotId != null) {
            System.out.printf("Snapshot ID   : %s%n", snapshotId);
        }
        System.out.printf("Files backed up: %,d%n", snapshotFiles.size());
        System.out.println("──────────────────────────────────────");

        return 0;
    }
//
//    private long countRegularFiles() throws IOException {
//        class Counter extends SimpleFileVisitor<Path> {
//            long count = 0;
//
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
//                if(shouldIncludeFile(file)) {
//                    count++;
//                }
//                return FileVisitResult.CONTINUE;
//            }
//        }
//
//        Counter c = new Counter();
//        Files.walkFileTree(sourceDir, c);
//        return c.count;
//    }
//
//    private boolean shouldIncludeFile(Path path) {
//        String rel = sourceDir.relativize(path).toString();
//
//        if (rel.startsWith(".") || rel.contains("/.")) {
//            return false;
//        }
//
//        if (rel.contains(".git") ||
//                rel.contains("node_modules") ||
//                rel.contains("target") ||
//                rel.contains("__pycache__") ||
//                rel.contains(".idea") ||
//                rel.contains(".vscode")) {
//            return false;
//        }
//
//        for(Pattern p : excludeRegexes) {
//            if(p.matcher(rel).matches()) {
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//        private void prepareExcludePatterns() {
//            for(String pat : excludePatterns) {
//                String regex = pat
//                        .replace(".", "\\.")
//                        .replace("**", ".+")
//                        .replace("*", "[^/]+")
//                        .replace("?", "[^/]+");
//                excludeRegexes.add(Pattern.compile(regex));
//            }
//        }

}
