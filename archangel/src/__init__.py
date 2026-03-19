import typer
import httpx

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


if __name__ == "__main__":
    app()