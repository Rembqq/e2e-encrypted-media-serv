package org.example.e2eencryptedmediaserv.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import org.example.e2eencryptedmediaserv.client.model.UploadMetadata;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Загальні утиліти для CLI-команд (upload, backup та майбутні).
 * Поки що статичні методи - для простоти.
 * Коли команд стане більше 5 → розглянути впровадження залежностей/сервіси.
 */

public class CliUtils {
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private CliUtils() {} // утилитный класс — не инстанцируем

    public static long countRegularFiles(Path root, List<Pattern> excludePatterns) throws IOException {
        class Counter extends SimpleFileVisitor<Path> {
            long count = 0;

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (shouldIncludeFile(root, file, excludePatterns)) {
                    count++;
                }
                return FileVisitResult.CONTINUE;
            }
        }
        Counter counter = new Counter();
        Files.walkFileTree(root, counter);
        return counter.count;
    }

    public static boolean shouldIncludeFile(Path root, Path path, List<Pattern> excludePatterns) {
        String rel = root.relativize(path).toString();

        // скрытые файлы и папки
        if (rel.startsWith(".") || rel.contains("/.")) {
            return false;
        }

        // типичные игнорируемые
        if (rel.contains(".git") || rel.contains("node_modules") || rel.contains("target") ||
                rel.contains("__pycache__") || rel.contains(".idea") || rel.contains(".vscode")) {
            return false;
        }

        // пользовательские exclude
        for (Pattern p : excludePatterns) {
            if (p.matcher(rel).matches()) {
                return false;
            }
        }
        return true;
    }

    public static List<Pattern> prepareExcludePatterns(List<String> patterns) {
        List<Pattern> regexes = new ArrayList<>();
        for (String pat : patterns) {
            String regex = pat
                    .replace(".", "\\.")
                    .replace("**", ".+")
                    .replace("*", "[^/]+")
                    .replace("?", "[^/]");
            regexes.add(Pattern.compile(regex));
        }
        return regexes;
    }

    public static String uploadBlob(String serverUrl, String token,
                                    byte[] blobData, String relativePathOrName,
                                    UploadMetadata metadata) throws Exception {

        String metadataJson = MAPPER.writeValueAsString(metadata);

        RequestBody blobBody = RequestBody.create(blobData, MediaType.get("application/octet-stream"));
        RequestBody metaBody = RequestBody.create(metadataJson, MediaType.get("application/json"));

        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("blob", relativePathOrName, blobBody)
                .addFormDataPart("metadata", "metadata.json", metaBody)
                .build();

        Request req = new Request.Builder()
                .url(serverUrl + "/api/v1/blobs")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response resp = HTTP_CLIENT.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "[no body]";
                System.err.printf("Upload failed %d: %s → %s%n", resp.code(), resp.message(), err);
                return null;
            }

            String responseBody = resp.body().string();
            System.out.println("Uploaded " + relativePathOrName + " → " + responseBody);

            // Простой парсинг id (замени на нормальный десериализацию позже)
            if (responseBody.contains("\"id\"")) {
                return responseBody.split("\"id\":\"")[1].split("\"")[0];
            }
            return "unknown-id";
        }
    }
}
