package org.archangel.system;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.*;

/**
 * FIXED: Critical deadlock resolved.
 *
 * ORIGINAL BUG: process.waitFor() was called BEFORE reading stdout/stderr.
 * If the process wrote more output than the OS pipe buffer (~64KB on Linux),
 * the process blocked waiting for the buffer to drain.
 * Java blocked in waitFor() waiting for the process to finish.
 * Deadlock. Process never exits. waitFor() times out and kills the process.
 *
 * FIX: stdout and stderr are drained concurrently by two reader threads
 * submitted to an executor BEFORE waitFor() is called. This matches the
 * standard Java process-reading pattern for production use.
 *
 * Also fixed: stream readers were previously opened AFTER waitFor() returned,
 * meaning output was read from an already-closed process — unreliable on some JVMs.
 */
@ApplicationScoped
public class SystemProcessExec {

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_CHARS = 10_000;

    private static final ExecutorService STREAM_POOL = Executors.newFixedThreadPool(4);
    public ProcessResult execute(List<String> commandParts) {
        return execute(commandParts, DEFAULT_TIMEOUT_SECONDS);
    }

    public ProcessResult execute(List<String> commandParts, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(commandParts);
            Process process = pb.start();

            // Start draining stdout and stderr concurrently BEFORE waitFor().
            // If we call waitFor() first and the process fills the pipe buffer,
            // it blocks forever — classic deadlock.
            Future<String> stdoutFuture = STREAM_POOL.submit(
                    () -> drainStream(new BufferedReader(new InputStreamReader(process.getInputStream())))
            );
            Future<String> stderrFuture = STREAM_POOL.submit(
                    () -> drainStream(new BufferedReader(new InputStreamReader(process.getErrorStream())))
            );

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);
                return new ProcessResult(-1, "", "Process timed out after " + timeoutSeconds + "s", true);
            }

            String stdout = getOrEmpty(stdoutFuture);
            String stderr = getOrEmpty(stderrFuture);

            return new ProcessResult(process.exitValue(), stdout, stderr, false);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "Process interrupted: " + e.getMessage(), false);
        } catch (Exception e) {
            return new ProcessResult(-1, "", "Process execution failed: " + e.getMessage(), false);
        }
    }

    private String drainStream(BufferedReader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() + line.length() > MAX_OUTPUT_CHARS) {
                    sb.append("\n...[OUTPUT TRUNCATED — exceeded ").append(MAX_OUTPUT_CHARS).append(" chars]");
                    break;
                }
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String getOrEmpty(Future<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }
}