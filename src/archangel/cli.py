import typer
import httpx
from ollama import chat
import questionary
import os
import threading
import itertools
import time

app = typer.Typer(
    help="ArchAngel — AI-powered system assistant for Arch Linux",
    add_completion=True,
    no_args_is_help=True
)

JAVA_RSS_URL = "http://127.0.0.1:9090/news"
JAVA_SYSTEM_URL = "http://127.0.0.1:9090/system"
model = "qwen2.5:3b"
API_KEY = os.getenv("ARCHANGEL_API_KEY", "dev-secret-key")
__version__ = "0.1.0"

@app.callback()
def main(
    verbose: bool = typer.Option(False, "--verbose", "-v"),
    json_output: bool = typer.Option(False, "--json"),
):
    """
    ArchAngel CLI
    """

@app.command()
def version():
    typer.echo(f"ArchAngel v{__version__}")


@app.command()
def scan_conf():
    """
    Scans your config files to see for any vulnerabilities
    """
    typer.echo("Checking any vulnerabilities")

@app.command()
def archnews():
    """
    Fetches and displays the latest Arch news from the ArchAngel service
    """
    try:
        typer.echo("Fetching news...")
        with httpx.Client(timeout=10) as client:
            r = client.get(JAVA_RSS_URL)
            typer.echo(f"Status: {r.status_code}")
            r.raise_for_status()
            news_items = r.json()

        if not news_items:
            typer.echo("No news items found.")
            raise typer.Exit()

        typer.echo(typer.style("\n=== Arch News ===\n", fg=typer.colors.CYAN, bold=True))

        for item in news_items:
            title = item.get("title", "No title")
            link  = item.get("link", "")
            date  = item.get("date", "")

            typer.echo(typer.style(f"• {title}", fg=typer.colors.GREEN, bold=True))
            if date:
                typer.echo(f"  Date: {date}")
            if link:
                typer.echo(f"  Link: {link}")
            typer.echo()

    except httpx.ConnectError:
        typer.echo(
            typer.style("Error: Could not connect to ArchAngel service. Is it running?", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)
    except httpx.HTTPStatusError as e:
        typer.echo(
            typer.style(f"Error: Service returned {e.response.status_code}", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)


@app.command()
def exec():
    '''Fetches info about your system'''
    try:
        typer.echo("Fetching data...")
        with httpx.Client(timeout=10) as client:
            r = client.post(
                f"{JAVA_SYSTEM_URL}/execute",
                content="uname -a",                        
                headers={"Content-Type": "text/plain", "X-Api-Key": API_KEY}
            )
            r.raise_for_status()
            response = r.json()                            

        exit_code = response.get("exitCode")
        stdout    = response.get("stdout", "")
        stderr    = response.get("stderr", "")

        if exit_code != 0:
            typer.echo(typer.style(f"Error: {stderr}", fg=typer.colors.RED), err=True)
            raise typer.Exit(code=1)

        typer.echo(typer.style("\n=== System Info ===\n", fg=typer.colors.CYAN, bold=True))
        typer.echo(stdout)

    except httpx.ConnectError:
        typer.echo(typer.style("Error: Could not connect to ArchAngel service. Is it running?", fg=typer.colors.RED), err=True)
        raise typer.Exit(code=1)
        
@app.command()
def get_status():
    '''
        Get status of your System.
    '''
    try:
        with httpx.Client() as client:
            r = client.get(f"{JAVA_SYSTEM_URL}/status",
            headers={"X-Api-Key": API_KEY}
            )
            typer.echo(f"Status: {r.status_code}")
            r.raise_for_status()
            system_info = r.json()

        if not system_info:
            typer.echo("No information can be found.")
            raise typer.Exit()

        typer.echo(system_info)

    except httpx.ConnectError:
        typer.echo(
            typer.style("Error: Could not connect to ArchAngel service. Is it running?", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)

    except httpx.HTTPStatusError as e:
        typer.echo(
            typer.style(f"Error: Service returned {e.response.status_code}", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)

@app.command()
def get_incidents():
    '''
        Fetches any incidents that occur.
    '''
    try:
        with httpx.Client(timeout=20) as client:
            r = client.get(f"{JAVA_SYSTEM_URL}/incidents",
            headers={"X-Api-Key": API_KEY}
            )
            r.raise_for_status()
            typer.echo(f"Status: {r.status_code}")
            incidents = r.json()

        if not incidents:
            typer.echo("Couldn't find any incidents.")
            raise typer.Exit()

        typer.echo(f"{incidents}")

    except httpx.ConnectError:
        typer.echo(
            typer.style("Error: Could not connect to ArchAngel service. Is it running?", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)
    except httpx.HTTPStatusError as e:
        typer.echo(
            typer.style(f"Error: Service returned {e.response.status_code}", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)

def thinking_spinner(stop_event):
    """
    Prints a simple thinking indicator without overwriting stdout.
    """
    while not stop_event.is_set():
        print("Thinking...", flush=True)
        time.sleep(0.8)


def chat_with_ollama(prompt: str, model=model) -> str:
    """
    Streams response from Ollama with live typing effect.
    """
    stop_event = threading.Event()
    spinner = threading.Thread(target=thinking_spinner, args=(stop_event,))
    spinner.start()

    stream = chat(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        stream=True
    )

    full_response = ""
    first_token = True

    for chunk in stream:
        content = chunk['message']['content']
        full_response += content

        # Stop spinner when first token arrives
        if first_token:
            stop_event.set()
            spinner.join()
            first_token = False
            print()  # separate spinner from output

        # Live typing effect
        for char in content:
            print(char, end="", flush=True)
            if char != " ":
                time.sleep(0.003)

    print()  # final newline
    return full_response

@app.command()
def update():
    '''Updates our system'''
    script = '''
    !/usr/bin/env bash
set -e
echo "🍽️ Feeding the system..."
# -------------------------------
# Helper functions
# -------------------------------
command_exists() {
    command -v "$1" >/dev/null 2>&1
}
run_safe() {
    echo "→ $1"
    bash -c "$1" || echo "⚠️ Failed: $1"
}
# -------------------------------
# Detect and update system packages
# -------------------------------
if command_exists pacman; then
    echo "[*] Detected Arch-based system"
    run_safe "sudo pacman -Syu --noconfirm"
    if command_exists yay; then
        run_safe "yay -Sua --noconfirm"
    elif command_exists paru; then
        run_safe "paru -Sua --noconfirm"
    fi
elif command_exists apt; then
    echo "[*] Detected Debian/Ubuntu"
    run_safe "sudo apt update"
    run_safe "sudo apt upgrade -y"
    run_safe "sudo apt autoremove -y"
elif command_exists dnf; then
    echo "[*] Detected Fedora/RHEL"
    run_safe "sudo dnf upgrade -y"
elif command_exists zypper; then
    echo "[*] Detected openSUSE"
    run_safe "sudo zypper refresh"
    run_safe "sudo zypper update -y"
else
    echo "⚠️ Unknown package manager. Skipping system update."
fi
# -------------------------------
# Flatpak
# -------------------------------
if command_exists flatpak; then
    run_safe "flatpak update -y"
fi
# -------------------------------
# Snap
# -------------------------------
if command_exists snap; then
    run_safe "sudo snap refresh"
fi
# -------------------------------
# npm (safe: user-level only)
# -------------------------------
if command_exists npm; then
    run_safe "npm update -g"
fi
# -------------------------------
# pipx (safe Python global tools)
# -------------------------------
if command_exists pipx; then
    run_safe "pipx upgrade-all"
fi
# -------------------------------
# Rust (cargo)
# -------------------------------
if command_exists cargo && command_exists cargo-install-update; then
    run_safe "cargo install-update -a"
fi
# -------------------------------
# Cleanup (only where applicable)
# -------------------------------
if command_exists pacman; then
    run_safe "sudo rm -rf /var/cache/pacman/pkg/download-*"
    run_safe "sudo pacman -Sc --noconfirm"
elif command_exists apt; then
    run_safe "sudo apt clean"
fi
echo "✅ System fed successfully."
'''
    os.system(script)


@app.command()
def summary():
    '''
    Shows you the summary of the problems that occured in you setup, their severity and what you should do to deal with them
    '''
    try:
        with httpx.Client(timeout=10) as client:
            r = client.post(
                f"{JAVA_SYSTEM_URL}/execute",
                content="journalctl -n 50", 
                headers={"Content-Type": "text/plain", "X-Api-Key": API_KEY}
            )
            r.raise_for_status()
            system_data = r.json()

        
        logs = system_data.get("stdout", "")
        hostname = system_data.get("hostname", "unknown") 
        timestamp = system_data.get("timestamp", "")

        if not logs:
            typer.echo("No logs retrieved.")
            raise typer.Exit()

        prompt = f"""
You are a Linux system assistant mainly for arch linux and related systems. Analyze the following logs and provide:
- Severity of specific logs(LOW/MEDIUM/HIGH/CRITICAL)
- Summary of what went wrong
- Recommended action

Hostname: {hostname}
Timestamp: {timestamp}
Logs:
{logs}
        """

        typer.echo("Analyzing logs...")
        ai_response = chat_with_ollama(prompt)


    except httpx.ConnectError:
        typer.echo(
            typer.style("Error: Could not connect to ArchAngel service. Is it running?", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)
    except httpx.HTTPStatusError as e:
        typer.echo(
            typer.style(f"Error: Service returned {e.response.status_code}", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)

@app.command()
def config_scan():
    '''
    Scans your Hyprland config files for any problems via the ArchAngel brain service.
    '''
    env = questionary.select(
        "Do you use Hyprland?",
        choices=["Yes", "No"]
    ).ask()

    if env != "Yes":
        typer.echo("Only Hyprland config scanning is supported right now.")
        raise typer.Exit()

    typer.echo("Checking system logs for problems first...")

    try:
        with httpx.Client(timeout=60) as client:
            r = client.post(
                f"{JAVA_SYSTEM_URL}/analyze",
                headers={"X-Api-Key": API_KEY}
            )

            # No logs available
            if r.status_code == 204:
                typer.echo("No logs available to analyze. Skipping config scan.")
                raise typer.Exit()

            r.raise_for_status()
            analysis = r.json()

        # Check if the brain flagged any problems
        analysis_text = analysis.get("content", "").lower()
        if not analysis_text or "no problems" in analysis_text or "no issues" in analysis_text:
            typer.echo(
                typer.style("✔ No problems detected in system logs. Config scan skipped.", fg=typer.colors.GREEN)
            )
            raise typer.Exit()

        typer.echo(
            typer.style("⚠ Problems detected in logs. Scanning Hyprland config files...", fg=typer.colors.YELLOW)
        )
        typer.echo(f"\nLog analysis summary:\n{analysis.get('content')}\n")

    except httpx.ConnectError:
        typer.echo(
            typer.style("Error: Could not connect to ArchAngel service. Is it running?", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)
    except httpx.HTTPStatusError as e:
        typer.echo(
            typer.style(f"Error: Service returned {e.response.status_code}", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)

    # Only reached if problems were found in logs
    hypr_path = os.path.expanduser("~/.config/hypr")
    found_any = False

    for root, dirs, files in os.walk(hypr_path):
        for file in files:
            filepath = os.path.join(root, file)

            try:
                with open(filepath, "r") as f:
                    content = f.read()
            except (OSError, UnicodeDecodeError) as e:
                typer.echo(typer.style(f"Skipping {file}: {e}", fg=typer.colors.YELLOW))
                continue

            try:
                with httpx.Client(timeout=120) as client:
                    r = client.post(
                        f"{JAVA_SYSTEM_URL}/analyze-config",
                        content=content,
                        headers={
                            "X-Api-Key": API_KEY,
                            "Content-Type": "text/plain"
                        }
                    )
                    r.raise_for_status()
                    result = r.json()
                    result_text = result.get("content", "")

                    # Only print files that actually have problems
                    if "no problems" in result_text.lower() or "no such problems" in result_text.lower():
                        continue

                    found_any = True
                    typer.echo(
                        typer.style(f"\n=== {file} ===\n", fg=typer.colors.CYAN, bold=True)
                    )
                    typer.echo(result_text)

            except httpx.HTTPStatusError as e:
                typer.echo(
                    typer.style(f"Error processing {file}: {e.response.status_code}", fg=typer.colors.RED),
                    err=True,
                )

    if not found_any:
        typer.echo(
            typer.style("\n✔ No config issues found across all Hyprland files.", fg=typer.colors.GREEN)
        )

@app.command()
def doctor():
    """
    Diagnose system + ArchAngel setup
    """
    checks = []

    # Check Java service
    try:
        httpx.get("http://127.0.0.1:9090/system/status", timeout=2)
        checks.append(("Java service", "OK"))
    except:
        checks.append(("Java service", "FAIL"))

    # Check Ollama
    try:
        chat(model="qwen3.5:9b", messages=[{"role": "user", "content": "ping"}])
        checks.append(("Ollama", "OK"))
    except:
        checks.append(("Ollama", "FAIL"))

    for name, status in checks:
        color = typer.colors.GREEN if status == "OK" else typer.colors.RED
        typer.echo(typer.style(f"{name}: {status}", fg=color))

if __name__ == "__main__":
    app()