package org.archangel.system;

import java.util.Set;

public class AllowedCommands
{
    public static final Set<String> Safe_Commands = Set.of(
            "uname -a",
            "free -h",
            "df -h",
            "uptime",
            "whoami",
            "hostname",
            "journalctl -n 50"
    );
    public static boolean isAllowed(String Command)
    {
        return Safe_Commands.contains(Command);
    }
}
