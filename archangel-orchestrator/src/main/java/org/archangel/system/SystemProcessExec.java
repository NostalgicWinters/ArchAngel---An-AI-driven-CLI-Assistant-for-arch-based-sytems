package org.archangel.system;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.List;
import java.util.concurrent.*;

@ApplicationScoped
public class SystemProcessExec {

    private static final int DEFAULT_TIMEOUT = 5;
    private static final int MAX_OUTPUT = 10000;

    public ProcessResult execute(List<String> commandParts) {
        return execute(commandParts, DEFAULT_TIMEOUT);
    }

    public ProcessResult execute(List<String> commandParts, int timeout) {

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
            Process process = processBuilder.start();

            Future<?> stdoutTask = executor.submit(() -> {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                    String line;

                    while ((line = reader.readLine()) != null) {

                        synchronized (stdout) {

                            if (stdout.length() + line.length() > MAX_OUTPUT) {
                                stdout.append("\n...[TRUNCATED]");
                                break;
                            }

                            stdout.append(line).append("\n");
                        }
                    }

                } catch (Exception ignored) {
                }
            });

            Future<?> stderrTask = executor.submit(() -> {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                    String line;

                    while ((line = reader.readLine()) != null) {

                        synchronized (stderr) {

                            if (stderr.length() + line.length() > MAX_OUTPUT) {
                                stderr.append("\n...[TRUNCATED]");
                                break;
                            }

                            stderr.append(line).append("\n");
                        }
                    }

                } catch (Exception ignored) {
                }
            });

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {

                process.destroyForcibly();

                return new ProcessResult(
                        -1,
                        "",
                        "Process timed out",
                        true
                );
            }

            stdoutTask.get();
            stderrTask.get();

            return new ProcessResult(
                    process.exitValue(),
                    stdout.toString(),
                    stderr.toString(),
                    false
            );

        } catch (Exception e) {

            return new ProcessResult(
                    -1,
                    "",
                    e.getMessage(),
                    false
            );

        } finally {
            executor.shutdown();
        }
    }
}