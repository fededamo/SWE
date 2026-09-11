#!/usr/bin/env python3
"""Build the deliverable HTML and PDF from the reviewed Markdown report."""

from __future__ import annotations

import html
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile

import markdown


ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "Relazione_SWE" / "relazione.md"
OUTPUT_DIR = ROOT / "Relazione_SWE"
HTML_OUTPUT = OUTPUT_DIR / "DriveHub_Relazione.html"
PDF_OUTPUT = OUTPUT_DIR / "DriveHub_Relazione.pdf"
REPORT_ASSETS = OUTPUT_DIR / "generated-assets"


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
.figure { page-break-inside: avoid; }
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
    rendered = prepare_report_images(rendered)
    # LibreOffice honours an explicit break more consistently than a break on
    # the cover container itself.
    rendered = rendered.replace(
        '</div>',
        '</div><p style="page-break-before: always"></p>',
        1,
    )
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
    # LibreOffice writes user configuration even for headless conversion.  A
    # disposable profile keeps the build independent from the desktop session
    # and makes it work in CI/sandboxed environments without touching $HOME.
    with tempfile.TemporaryDirectory(prefix="drivehub-report-") as profile, \
            tempfile.TemporaryDirectory(prefix=".report-build-", dir=OUTPUT_DIR) as conversion:
        profile_path = pathlib.Path(profile)
        runtime = profile_path / "runtime"
        config = profile_path / "config"
        cache = profile_path / "cache"
        for directory in (runtime, config, cache):
            directory.mkdir()
        environment = os.environ.copy()
        environment.update({
            "XDG_RUNTIME_DIR": str(runtime),
            "XDG_CONFIG_HOME": str(config),
            "XDG_CACHE_HOME": str(cache),
        })
        html_to_odt = [
            soffice,
            "--headless",
            f"-env:UserInstallation={profile_path.as_uri()}",
            "--convert-to",
            "odt",
            "--outdir",
            conversion,
            str(HTML_OUTPUT),
        ]
        completed = subprocess.run(
            html_to_odt,
            cwd=OUTPUT_DIR,
            text=True,
            capture_output=True,
            env=environment,
        )
        odt = pathlib.Path(conversion) / f"{HTML_OUTPUT.stem}.odt"
        if completed.returncode != 0 or not odt.exists():
            detail = (completed.stdout + "\n" + completed.stderr).strip()
            print(f"ODT conversion failed: {html.escape(detail)}", file=sys.stderr)
            return completed.returncode or 4
        resize_odt_images(odt)
        odt_to_pdf = [
            soffice,
            "--headless",
            f"-env:UserInstallation={profile_path.as_uri()}",
            "--convert-to",
            "pdf:writer_pdf_Export",
            "--outdir",
            conversion,
            str(odt),
        ]
        completed = subprocess.run(
            odt_to_pdf,
            cwd=OUTPUT_DIR,
            text=True,
            capture_output=True,
            env=environment,
        )
        converted = pathlib.Path(conversion) / PDF_OUTPUT.name
        if completed.returncode != 0 or not converted.exists():
            detail = (completed.stdout + "\n" + completed.stderr).strip()
            print(f"PDF conversion failed: {html.escape(detail)}", file=sys.stderr)
            return completed.returncode or 4
        converted.replace(PDF_OUTPUT)
    print(PDF_OUTPUT)
    return 0


def prepare_report_images(rendered: str) -> str:
    """Create A4-sized assets because Writer ignores CSS max-width on import."""
    REPORT_ASSETS.mkdir(exist_ok=True)
    image_tool = shutil.which("magick") or shutil.which("convert")

    def replace(match: re.Match[str]) -> str:
        prefix, reference, suffix = match.groups()
        source = (OUTPUT_DIR / reference).resolve()
        try:
            source.relative_to(ROOT)
        except ValueError as failure:
            raise ValueError(f"Report image outside project: {reference}") from failure
        if not source.exists():
            raise FileNotFoundError(f"Missing report image: {source}")

        if image_tool is None:
            raise RuntimeError("ImageMagick is required to size report images")
        destination = REPORT_ASSETS / source.name
        is_diagram = "diagrams/rendered" in source.as_posix()
        # Writer honours PNG physical density, while it ignores CSS max-width
        # and SVG viewBox dimensions during HTML import.  A 225 dpi diagram
        # preserves enough pixels for zooming and occupies at most 440 pt on
        # paper; screenshots retain their native pixels at 180 dpi.
        geometry = "1375x2030>" if is_diagram else "1100x1625>"
        density = "225" if is_diagram else "180"
        subprocess.run(
            [image_tool, str(source), "-resize", geometry,
             "-units", "PixelsPerInch", "-density", density, str(destination)],
            check=True,
            text=True,
            capture_output=True,
        )
        relative = destination.relative_to(OUTPUT_DIR).as_posix()
        physical_width = "90mm" if source.name == "payment-dialog.png" else "165mm"
        prefix = prefix.replace(
            "<img ",
            f'<img style="width:{physical_width};height:auto" ',
            1,
        )
        return f'{prefix}{relative}{suffix}'

    return re.sub(r'(<img[^>]*\bsrc=")([^"]+)(")', replace, rendered)


def resize_odt_images(odt: pathlib.Path) -> None:
    """Constrain imported image frames while preserving aspect ratio/pixels."""
    svg_namespace = "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
    draw_namespace = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
    width_attribute = f"{{{svg_namespace}}}width"
    height_attribute = f"{{{svg_namespace}}}height"
    image_element = f"{{{draw_namespace}}}image"

    with zipfile.ZipFile(odt, "r") as archive:
        content = archive.read("content.xml")
        tree = ET.fromstring(content)
        changed = False
        for frame in tree.iter(f"{{{draw_namespace}}}frame"):
            if frame.find(f".//{image_element}") is None:
                continue
            width = measurement_cm(frame.get(width_attribute, ""))
            height = measurement_cm(frame.get(height_attribute, ""))
            if width is None or height is None or width <= 0 or height <= 0:
                continue
            scale = min(1.0, 16.5 / width, 23.0 / height)
            if scale < 1.0:
                frame.set(width_attribute, f"{width * scale:.3f}cm")
                frame.set(height_attribute, f"{height * scale:.3f}cm")
                changed = True
        if not changed:
            return
        resized_content = ET.tostring(tree, encoding="utf-8", xml_declaration=True)
        temporary = odt.with_suffix(".resized.odt")
        with zipfile.ZipFile(temporary, "w") as output:
            for item in archive.infolist():
                data = resized_content if item.filename == "content.xml" else archive.read(item.filename)
                output.writestr(item, data)
    temporary.replace(odt)


def measurement_cm(value: str) -> float | None:
    match = re.fullmatch(r"([0-9.]+)(cm|mm|in|pt)", value)
    if match is None:
        return None
    number = float(match.group(1))
    return number * {"cm": 1.0, "mm": 0.1, "in": 2.54, "pt": 2.54 / 72.0}[match.group(2)]


if __name__ == "__main__":
    raise SystemExit(main())
