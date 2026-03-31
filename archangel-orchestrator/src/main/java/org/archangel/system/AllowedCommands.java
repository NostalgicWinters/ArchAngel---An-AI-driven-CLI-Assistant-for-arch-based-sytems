package org.archangel.system;

import java.util.Set;

/**
 * FIXED: Added journalctl commands required by SystemLogService and the /analyze flow.
 *
 * ORIGINAL BUG: summary() in the CLI sent "journalctl -n 50" to /system/execute,
 * which was not in Safe_Commands — the command was silently rejected with exitCode=-1,
 * causing summary() to always fail with no useful error message.
 *
 * Also fixed: Java naming convention. Class and constant names corrected.
 *
 * NOTE ON SECURITY: This allowlist is a defense-in-depth layer, not the primary
 * security boundary. The primary boundary is that /system/execute must never be
 * exposed outside localhost. These commands are all read-only and produce no
 * side effects on the system.
 */
public final class AllowedCommands {

    private AllowedCommands() {}

    public static final Set<String> SAFE_COMMANDS = Set.of(
            // System identity
            "uname -a",
            "hostname",
            "whoami",

            // Resource status
            "free -h",
            "df -h",
            "uptime",

            // Log retrieval — required by SystemLogService and the AI analysis pipeline
            "journalctl -p 3..5 -n 50",   // errors+warnings, last 50 (SystemLogService default)
            "journalctl -n 50",            // last 50 all priorities (used by CLI summary)
            "journalctl -n 100",           // extended fetch for deeper analysis
            "journalctl -b -p 3",          // errors since last boot

            "journalctl -p 3..5 -n 50 --no-pager --output=short-iso",

            // Package state — useful for security audits
            "pacman -Q",                   // list installed packages
            "pacman -Qu"                   // list upgradable packages
    );

    public static boolean isAllowed(String command) {
        if (command == null) return false;
        return SAFE_COMMANDS.contains(command.trim());
    }
}