import typer
import httpx

app = typer.Typer()

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
    FASTAPI_BASE_URL = "http://localhost:8000"
    """
    Fetches and displays the latest Arch news from the ArchAngel service
    """
    try:
        typer.echo("Fetching news...")
        with httpx.Client(timeout=10) as client:
            r = client.get(f"{FASTAPI_BASE_URL}/archnews")
            typer.echo(f"Status: {r.status_code}")
            typer.echo(f"Raw response: {r.text}")
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

if __name__ == "__main__":   # <-- this was missing
    app()