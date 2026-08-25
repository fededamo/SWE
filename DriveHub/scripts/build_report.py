#!/usr/bin/env python3
"""Build the deliverable HTML and PDF from the reviewed Markdown report."""

from __future__ import annotations

import html
import pathlib
import shutil
import subprocess
import sys

import markdown


ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "Relazione_SWE" / "relazione.md"
OUTPUT_DIR = ROOT / "Relazione_SWE"
HTML_OUTPUT = OUTPUT_DIR / "DriveHub_Relazione.html"
PDF_OUTPUT = OUTPUT_DIR / "DriveHub_Relazione.pdf"


STYLE = """
@page { size: A4; margin: 18mm 17mm 20mm; }
html { color: #172033; background: white; }
body {
  font-family: "Liberation Sans", Arial, sans-serif;
  font-size: 10.5pt;
  line-height: 1.45;
  max-width: 178mm;
  margin: 0 auto;
}
h1, h2, h3 { color: #12355b; line-height: 1.2; page-break-after: avoid; }
h1 { font-size: 28pt; margin-top: 34mm; }
h2 { font-size: 19pt; border-bottom: 2px solid #2c7da0; padding-bottom: 5px; margin-top: 18px; }
h3 { font-size: 13.5pt; margin-top: 17px; }
p, li { orphans: 3; widows: 3; }
a { color: #176b87; text-decoration: none; }
blockquote { border-left: 4px solid #2c7da0; margin-left: 0; padding: 8px 15px; background: #eef7fa; }
table { border-collapse: collapse; width: 100%; font-size: 8.5pt; margin: 10px 0 16px; page-break-inside: auto; }
thead { display: table-header-group; }
tr { page-break-inside: avoid; }
th { background: #12355b; color: white; font-weight: 600; }
th, td { border: 1px solid #aebdca; padding: 5px 6px; vertical-align: top; }
tr:nth-child(even) td { background: #f5f8fa; }
code { font-family: "Liberation Mono", monospace; background: #edf1f4; padding: 1px 3px; }
pre { background: #172033; color: #f6f8fa; padding: 10px; overflow-wrap: anywhere; white-space: pre-wrap; page-break-inside: avoid; }
pre code { background: transparent; color: inherit; padding: 0; }
img { display: block; max-width: 100%; max-height: 225mm; margin: 12px auto 5px; object-fit: contain; }
.caption { text-align: center; color: #526477; font-size: 8.5pt; margin: 0 0 14px; }
.cover { min-height: 245mm; text-align: center; page-break-after: always; }
.cover h1 { margin-top: 45mm; }
.report-meta { color: #526477; font-size: 9pt; }
"""


def main() -> int:
    if not SOURCE.exists():
        print(f"Missing report source: {SOURCE}", file=sys.stderr)
        return 2
    source = SOURCE.read_text(encoding="utf-8")
    rendered = markdown.markdown(
        source,
        extensions=["extra", "sane_lists", "smarty", "toc"],
        output_format="html5",
    )
    # The source deliberately uses a centered first div; give it cover semantics.
    rendered = rendered.replace('<div align="center">', '<div align="center" class="cover">', 1)
    document = f"""<!doctype html>
<html lang="it">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>DriveHub — Relazione SWE</title>
  <style>{STYLE}</style>
</head>
<body>
{rendered}
</body>
</html>
"""
    HTML_OUTPUT.write_text(document, encoding="utf-8")

    soffice = shutil.which("soffice")
    if soffice is None:
        print(f"HTML generated at {HTML_OUTPUT}; LibreOffice (soffice) is unavailable.")
        return 3
    command = [
        soffice,
        "--headless",
        "--convert-to",
        "pdf:writer_pdf_Export",
        "--outdir",
        str(OUTPUT_DIR),
        str(HTML_OUTPUT),
    ]
    completed = subprocess.run(command, cwd=OUTPUT_DIR, text=True, capture_output=True)
    if completed.returncode != 0 or not PDF_OUTPUT.exists():
        detail = (completed.stdout + "\n" + completed.stderr).strip()
        print(f"PDF conversion failed: {html.escape(detail)}", file=sys.stderr)
        return completed.returncode or 4
    print(PDF_OUTPUT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
