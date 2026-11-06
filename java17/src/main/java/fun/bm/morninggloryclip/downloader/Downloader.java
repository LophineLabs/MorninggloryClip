package fun.bm.morninggloryclip.downloader;

import fun.bm.morninggloryclip.update.AutoUpdate;
import io.papermc.paperclip.FileEntry;
import io.papermc.paperclip.Paperclip;
import io.papermc.paperclip.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leavesclip.logger.SimpleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.nio.file.StandardOpenOption.*;

public record Downloader(FileEntry entry, Path outputDir, Path outputFile, String baseDir, Path originalRootDir,
                         boolean useInternal) {
    private static final SimpleLogger logger = new SimpleLogger("MorninggloryClip");

    private static final int ATTEMPTS_PER_REPOSITORY = 2;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Contract("_ -> new")
    public @NotNull CompletableFuture<Path> downloadOrLoad(Executor worker) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Try loading : " + this.entry.id());

            final RuntimeException failed = new RuntimeException("All maven repo download attempts has been failed for library " + this.entry.id() + "!");

            try {
                final String filePath = Util.endingSlash(this.baseDir) + this.entry.path();
                InputStream fileStream = this.useInternal ? AutoUpdate.getResourceAsStreamFromTargetJar(filePath) : null;
                if (fileStream == null && this.useInternal) {
                    if (this.originalRootDir != null) {
                        final Path originalFile = originalRootDir.resolve(filePath);
                        if (Files.exists(originalFile)) {
                            fileStream = Files.newInputStream(originalFile);
                        }
                    }
                }

                if (fileStream != null) {
                    logger.info("Located target jar inside jar, loading.");
                    this.deleteIfInvalid();
                    this.write(fileStream);
                    logger.info("Loaded " + this.entry.id() + " from jar package locally.");
                    return this.outputFile;
                }
            } catch (Exception ex) {
                failed.addSuppressed(ex);
            }

            logger.info("Missing: " + this.entry.id() + ", downloading from maven repo.");

            final List<String> repositories = Paperclip.getMavenRepositories();

            // libraries.list already contains the path in maven layout, the file must match the listed SHA-256
            for (final String repository : repositories) {
                final String url = repository + this.entry.path();
                try {
                    if (this.downloadVerified(url)) {
                        logger.info("Downloaded " + this.entry.id() + " from " + repository);
                        return this.outputFile;
                    }
                } catch (Exception ex) {
                    failed.addSuppressed(ex);
                }
            }

            // Fallback: resolve by coordinate (e.g. the path differs from maven layout), still checked by SHA-256
            try {
                this.deleteIfInvalid();

                final MavenDependencyResolver resolver = new MavenDependencyResolver(
                        repositories.stream().map(url -> new MavenDependencyResolver.MavenRepository(String.valueOf(url.hashCode()), url)).toList(),
                        this.outputDir
                );

                resolver.downloadTo(this.entry.id(), this.outputFile);
                if (Util.isFileValid(this.outputFile, this.entry.hash())) {
                    return this.outputFile;
                }
                Files.deleteIfExists(this.outputFile);
                failed.addSuppressed(new IOException("SHA-256 mismatch for " + this.entry.id() + " resolved by coordinate"));
            } catch (Exception ex) {
                failed.addSuppressed(ex);
            }

            throw failed;
        }, worker);
    }

    /**
     * @return true if the file was downloaded and matches the expected SHA-256, false if the repository doesn't have it
     * @throws IOException if the file can't be downloaded or doesn't match
     */
    private boolean downloadVerified(final String url) throws IOException, InterruptedException {
        Files.createDirectories(this.outputFile.getParent());
        final Path temp = this.outputFile.resolveSibling(this.outputFile.getFileName() + ".part");

        IOException failure = null;
        for (int attempt = 0; attempt < ATTEMPTS_PER_REPOSITORY; attempt++) {
            try {
                final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .header("User-Agent", "MorninggloryClip")
                        .GET()
                        .build();
                final HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (final InputStream body = response.body()) {
                    final int status = response.statusCode();
                    if (status == 404 || status == 403 || status == 410) {
                        return false;
                    }
                    if (status != 200) {
                        throw new IOException("HTTP " + status + " from " + url);
                    }

                    final MessageDigest digest = Util.newSha256Digest();
                    try (final OutputStream out = Files.newOutputStream(temp, CREATE, WRITE, TRUNCATE_EXISTING)) {
                        final byte[] buffer = new byte[64 * 1024];
                        int read;
                        while ((read = body.read(buffer)) != -1) {
                            digest.update(buffer, 0, read);
                            out.write(buffer, 0, read);
                        }
                    }

                    final byte[] actual = digest.digest();
                    if (!Arrays.equals(actual, this.entry.hash())) {
                        Files.deleteIfExists(temp);
                        throw new IOException("SHA-256 mismatch from " + url + ": expected "
                                + HexFormat.of().formatHex(this.entry.hash()) + ", got " + HexFormat.of().formatHex(actual));
                    }

                    Files.move(temp, this.outputFile, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            } catch (IOException ex) {
                Files.deleteIfExists(temp);
                failure = ex;
                if (ex.getMessage() != null && ex.getMessage().startsWith("SHA-256 mismatch")) {
                    break; // retrying the same repository won't help
                }
            }
        }
        throw failure;
    }

    private void deleteIfInvalid() throws IOException {
        if (!Files.isDirectory(this.outputFile.getParent())) {
            Files.createDirectories(this.outputFile.getParent());
        }
        Files.deleteIfExists(this.outputFile);
    }

    private void write(InputStream in) throws IOException {
        try (
                final InputStream stream = in;
                final ReadableByteChannel inputChannel = Channels.newChannel(stream);
                final FileChannel outputChannel = FileChannel.open(this.outputFile, CREATE, WRITE, TRUNCATE_EXISTING)
        ) {
            outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
        }
    }
}
