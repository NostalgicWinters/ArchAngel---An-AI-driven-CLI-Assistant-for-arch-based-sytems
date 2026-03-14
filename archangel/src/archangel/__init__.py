import typer


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