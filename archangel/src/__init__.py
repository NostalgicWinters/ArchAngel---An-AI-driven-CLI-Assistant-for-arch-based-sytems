import typer
import httpx
import questionary
import os

app = typer.Typer()

JAVA_RSS_URL = "http://127.0.0.1:9090/news"
JAVA_SYSTEM_URL = "http://127.0.0.1:9090/system"
JAVA_ANALYSIS_URL = "http://127.0.0.1:9090/analysis"

API_KEY = os.getenv("ARCHANGEL_API_KEY", "dev-secret-key")


@app.callback()
def callback():
    """
    ArchAngel — Arch Linux system assistant.
    CLI → Java → Python brain (Groq)
    """


# ── Helpers ─────────────────────────────────────────────────────

def _auth_headers() -> dict:
    return {"X-Api-Key": API_KEY}


def _severity_color(severity: str) -> str:
    return {
        "CRITICAL": typer.colors.RED,
        "HIGH": typer.colors.BRIGHT_RED,
        "MEDIUM": typer.colors.YELLOW,
        "LOW": typer.colors.GREEN,
    }.get(severity.upper(), typer.colors.WHITE)


def _print_analysis(analysis: dict) -> None:
    severity = analysis.get("severity", "UNKNOWN")
    summary = analysis.get("summary", "")
    action = analysis.get("recommendedAction", "")
    valid = analysis.get("analysisValid", False)

    if not valid:
        typer.echo(
            typer.style(
                "⚠  AI analysis unavailable — brain service is down.\n"
                "   Run: systemctl status archangel-brain",
                fg=typer.colors.YELLOW,
            )
        )

    typer.echo(typer.style("\n=== Log Analysis ===\n", fg=typer.colors.CYAN, bold=True))
    typer.echo(f"Severity:  {typer.style(severity, fg=_severity_color(severity), bold=True)}")
    typer.echo(f"Summary:   {summary}")
    if action:
        typer.echo(f"Action:    {action}")


# 🧠 NEW: call FastAPI brain (Groq inside)
def call_brain_api(logs: str) -> dict:
    try:
        with httpx.Client(timeout=20) as client:
            r = client.post(
                "http://127.0.0.1:8000/analyze",
                json={"logs": logs},
            )
            r.raise_for_status()
            return r.json()
    except Exception as e:
        return {
            "severity": "UNKNOWN",
            "summary": f"Fallback API failed: {e}",
            "recommendedAction": "Check FastAPI brain service",
            "analysisValid": False,
        }


# ── Commands ───────────────────────────────────────────────────

@app.command()
def summary():
    """
    CLI → Java → Python brain (Groq)
    Fallback: direct FastAPI call (NO OLLAMA)
    """
    try:
        typer.echo("Collecting and analyzing system logs...")

        with httpx.Client(timeout=60) as client:
            r = client.post(
                f"{JAVA_SYSTEM_URL}/analyze",
                headers=_auth_headers(),
            )
            r.raise_for_status()
            analysis = r.json()

    except Exception:
        typer.echo(
            typer.style("Error: Java service not reachable", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)

    _print_analysis(analysis)

    if analysis.get("analysisValid", False):
        raise typer.Exit()

    # 🔥 NEW FALLBACK (NO OLLAMA)
    typer.echo(
        typer.style(
            "\nFalling back to direct brain API (Groq)...",
            fg=typer.colors.YELLOW,
        )
    )

    try:
        with httpx.Client(timeout=10) as client:
            r = client.post(
                f"{JAVA_SYSTEM_URL}/execute",
                content="journalctl -p 3..5 -n 50 --no-pager --output=short-iso",
                headers={"Content-Type": "text/plain", **_auth_headers()},
            )
            r.raise_for_status()
            system_data = r.json()

        logs = system_data.get("stdout", "")
        if not logs.strip():
            typer.echo("No logs found.")
            raise typer.Exit()

        typer.echo("Analyzing via Groq...")

        result = call_brain_api(logs)
        _print_analysis(result)

    except Exception as e:
        typer.echo(
            typer.style(f"Fallback failed: {e}", fg=typer.colors.RED),
            err=True,
        )
        raise typer.Exit(code=1)


# ── HEALTH ───────────────────────────────────────────────────

@app.command()
def health():
    typer.echo("CLI is alive. If AI fails, it's your backend, not me.")


if __name__ == "__main__":
    app()