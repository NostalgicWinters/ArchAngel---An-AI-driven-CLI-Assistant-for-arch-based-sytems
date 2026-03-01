package org.archangel.system;

import io.vertx.core.spi.launcher.Command;
import jakarta.enterprise.context.ApplicationScoped;
import org.archangel.model.CommandResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@ApplicationScoped
public class CommandService
{


    public CommandResponse executeCommand(String command) {
        try{
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c",  command);

        Process process = processBuilder.start();
        BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        String line;
        while((line = stdOut.readLine()) != null){
            out.append(line).append("\n");
        }
        while((line = stdErr.readLine()) != null){
            err.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        return new CommandResponse(command,exitCode,out.toString(),err.toString());
    }catch (Exception e)
        {
            return new CommandResponse(command, -1, "", e.getMessage());
        }
    }
}
