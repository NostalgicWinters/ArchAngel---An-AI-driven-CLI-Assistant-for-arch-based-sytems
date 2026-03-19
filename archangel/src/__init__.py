import typer
import httpx
from ollama import chat
import questionary
import os

app = typer.Typer()

JAVA_RSS_URL = "http://127.0.0.1:9090/news"
JAVA_SYSTEM_URL = "http://127.0.0.1:9090/system"

@app.callback()
def callback():
    """
    This is ArchAngel
    """


@app.command()
def locate_conf():
    """
    This command scans your system files to find your config files
    """
    typer.echo("Locating config files")


@app.command()
def scan_conf():
    """
    Scans your config files to see for any vulnerabilities
    """
    typer.echo("Checking any vulnerabilities")

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
                headers={"Content-Type": "text/plain"}
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
            r = client.get(f"{JAVA_SYSTEM_URL}/status")
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
        with httpx.Client() as client:
            r = client.get(f"{JAVA_SYSTEM_URL}/incidents")
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

def chat_with_ollama(prompt: str, model="qwen3.5:9b") -> str:
    try:
        messages = [{"role": "user", "content": prompt}]
        result = chat(model=model, messages=messages)
        return result['message']['content']
    except Exception as e:
        typer.echo(typer.style(f"Ollama error: {e}", fg=typer.colors.RED), err=True)
        return ""

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
                headers={"Content-Type": "text/plain"}
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
You are a Linux system assistant. Analyze the following logs and provide:
- Severity (LOW/MEDIUM/HIGH/CRITICAL)
- Summary of what went wrong
- Recommended action

Hostname: {hostname}
Timestamp: {timestamp}
Logs:
{logs}
        """

        typer.echo("Analyzing logs...")
        ai_response = chat_with_ollama(prompt)

        typer.echo(typer.style("\n=== Recommended Action ===\n", fg=typer.colors.CYAN, bold=True))
        typer.echo(ai_response)

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
        Scans your config files for any problems.
    '''
    env = questionary.select(
        "Which desktop environment do you use?",
        choices=[
            "KDE",
            "Hyprland",
            "GNOME",
            "Other"
        ]
    ).ask()

    typer.echo(f"You selected: {env}")

    if env == "Hyprland":

        for root, dirs, files in os.walk(os.path.expanduser("~/.config/hypr")):
            for file in files:
                print(os.path.join(root, file))

if __name__ == "__main__":
    app()