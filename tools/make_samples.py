"""Gera livros de teste em samples/ (fora do git): um EPUB 3 e um PDF.

O EPUB cobre o que a Fase 0 precisa validar: sumário aninhado, notas de rodapé
(epub:type="noteref"), blocos de código e tabela. Texto original, sem direitos de terceiros.

Uso: python tools/make_samples.py
"""

import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "samples"

CHAPTERS = [
    (
        "cap1",
        "1. O problema da forma",
        [
            ("Forma e conteúdo", [
                "Todo texto técnico carrega duas camadas: o que ele afirma e a maneira como organiza "
                "essas afirmações. Ler bem é perceber as duas ao mesmo tempo.",
                "Quem estuda um livro difícil raramente o lê de uma vez. Volta, sublinha, compara "
                "trechos distantes e escreve na margem o que ainda não entendeu.",
            ]),
            ("Leitura lenta", [
                "A leitura lenta não é um defeito do leitor. É o ritmo natural de quem pretende "
                "lembrar e usar o que leu.",
            ]),
        ],
    ),
    (
        "cap2",
        "2. Ler código como texto",
        [
            ("Intenções", [
                "Lemos código para reconstruir intenções, não para executá-lo de cabeça. Um bom "
                "nome encurta a distância entre o texto e a teoria que o produziu.",
            ]),
        ],
    ),
    (
        "cap3",
        "3. Programar é construir teoria",
        [
            ("A teoria de um programa", [
                "Um programa não é apenas o texto que o descreve. Quando a equipe que o escreveu se "
                "dispersa, perde-se algo que nenhum comentário recupera: a compreensão de por que "
                "cada parte é como é.",
                "Peter Naur chamou essa compreensão de teoria.FN1 Ter a teoria de um programa é saber "
                "explicar suas escolhas, responder a perguntas sobre ele e modificá-lo sem trair sua forma.",
                "CODE",
                "Daí uma consequência incômoda: a documentação ajuda, mas não substitui a convivência "
                "com o código. A teoria mora nas pessoas, e é com elas que se transmite.FN2",
            ]),
            ("Morte e revivência de programas", [
                "TABLE",
                "Um programa sem ninguém que detenha sua teoria pode continuar funcionando por anos. "
                "O problema aparece no dia em que alguém precisa mudá-lo.",
            ]),
        ],
    ),
]

FOOTNOTES = {
    "FN1": "Peter Naur, “Programming as Theory Building”, Microprocessing and Microprogramming 15, "
           "n. 5 (1985), p. 253–261.",
    "FN2": "A mesma ideia aparece, com outros nomes, em toda tradição de ofício: o aprendiz aprende "
           "trabalhando ao lado de quem sabe.",
}

CODE = """<pre><code>def revisar(programa, teoria):
    \"\"\"Devolve o que a teoria explica do programa.\"\"\"
    return [parte for parte in programa if teoria.explica(parte)]</code></pre>"""

TABLE = """<table>
<caption>Estados de um programa</caption>
<thead><tr><th>Estado</th><th>Quem detém a teoria</th><th>Risco de mudança</th></tr></thead>
<tbody>
<tr><td>Vivo</td><td>A equipe original</td><td>Baixo</td></tr>
<tr><td>Em transição</td><td>Parte da equipe</td><td>Médio</td></tr>
<tr><td>Morto</td><td>Ninguém</td><td>Alto</td></tr>
</tbody>
</table>"""

CSS = """body { font-family: serif; }
h1 { font-size: 1.5em; margin-top: 2em; }
pre { font-family: monospace; white-space: pre-wrap; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid currentColor; padding: 0.3em 0.5em; text-align: left; }
aside { display: none; }
"""


def chapter_xhtml(slug, title, sections):
    body = [f'<h1 id="top">{title}</h1>']
    notes = []
    for index, (heading, paragraphs) in enumerate(sections):
        body.append(f'<h2 id="s{index}">{heading}</h2>')
        for paragraph in paragraphs:
            if paragraph == "CODE":
                body.append(CODE)
                continue
            if paragraph == "TABLE":
                body.append(TABLE)
                continue
            for key, text in FOOTNOTES.items():
                if key in paragraph:
                    number = key[-1]
                    paragraph = paragraph.replace(
                        key,
                        f'<a epub:type="noteref" href="#{key.lower()}" id="ref-{key.lower()}"><sup>{number}</sup></a>',
                    )
                    notes.append(f'<aside epub:type="footnote" id="{key.lower()}"><p>{text}</p></aside>')
            body.append(f"<p>{paragraph}</p>")
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="pt-BR" lang="pt-BR">
<head><meta charset="utf-8"/><title>{title}</title><link rel="stylesheet" href="style.css"/></head>
<body>
{chr(10).join(body)}
{chr(10).join(notes)}
</body>
</html>
"""


def nav_xhtml():
    items = []
    for slug, title, sections in CHAPTERS:
        sub = "".join(f'<li><a href="{slug}.xhtml#s{i}">{h}</a></li>' for i, (h, _) in enumerate(sections))
        items.append(f'<li><a href="{slug}.xhtml">{title}</a><ol>{sub}</ol></li>')
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="pt-BR" lang="pt-BR">
<head><meta charset="utf-8"/><title>Sumário</title></head>
<body><nav epub:type="toc" id="toc"><h1>Sumário</h1><ol>{''.join(items)}</ol></nav></body>
</html>
"""


def opf():
    manifest = "\n".join(
        f'<item id="{slug}" href="{slug}.xhtml" media-type="application/xhtml+xml"/>' for slug, _, _ in CHAPTERS
    )
    spine = "\n".join(f'<itemref idref="{slug}"/>' for slug, _, _ in CHAPTERS)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid" xml:lang="pt-BR">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="uid">urn:uuid:6f1c1d1e-2b0a-4c39-9a1e-marginalia-sample</dc:identifier>
<dc:title>Notas sobre o método</dc:title>
<dc:creator>Ana Ferraz</dc:creator>
<dc:language>pt-BR</dc:language>
<meta property="dcterms:modified">2026-11-01T00:00:00Z</meta>
</metadata>
<manifest>
<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
<item id="css" href="style.css" media-type="text/css"/>
{manifest}
</manifest>
<spine>
{spine}
</spine>
</package>
"""


def write_epub(path):
    with zipfile.ZipFile(path, "w") as epub:
        epub.writestr(zipfile.ZipInfo("mimetype"), "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        epub.writestr(
            "META-INF/container.xml",
            '<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">'
            '<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>'
            "</rootfiles></container>",
            compress_type=zipfile.ZIP_DEFLATED,
        )
        epub.writestr("OEBPS/content.opf", opf(), compress_type=zipfile.ZIP_DEFLATED)
        epub.writestr("OEBPS/nav.xhtml", nav_xhtml(), compress_type=zipfile.ZIP_DEFLATED)
        epub.writestr("OEBPS/style.css", CSS, compress_type=zipfile.ZIP_DEFLATED)
        for slug, title, sections in CHAPTERS:
            epub.writestr(f"OEBPS/{slug}.xhtml", chapter_xhtml(slug, title, sections), compress_type=zipfile.ZIP_DEFLATED)


def write_pdf(path, pages):
    """PDF mínimo, só texto em Helvetica, uma página por item de `pages`."""
    objects = []

    def add(content):
        objects.append(content)
        return len(objects)

    font = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
    pages_id = len(objects) + 1 + 2 * len(pages)
    kids = []
    for lines in pages:
        stream = "BT /F1 14 Tf 72 760 Td 20 TL " + " ".join(
            "(" + line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") + ") '" for line in lines
        ) + " ET"
        data = stream.encode("cp1252")
        content = add(b"<< /Length %d >>\nstream\n" % len(data) + data + b"\nendstream")
        kids.append(add(
            b"<< /Type /Page /Parent %d 0 R /MediaBox [0 0 612 792] /Contents %d 0 R "
            b"/Resources << /Font << /F1 %d 0 R >> >> >>" % (pages_id, content, font)
        ))
    add(b"<< /Type /Pages /Kids [" + b" ".join(b"%d 0 R" % k for k in kids) + b"] /Count %d >>" % len(kids))
    catalog = add(b"<< /Type /Catalog /Pages %d 0 R >>" % pages_id)
    info = add("<< /Title (Caderno de exercícios) /Author (Marginália) >>".encode("cp1252"))

    out = bytearray(b"%PDF-1.4\n")
    offsets = []
    for number, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += b"%d 0 obj\n" % number + body + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 %d\n0000000000 65535 f \n" % (len(objects) + 1)
    for offset in offsets:
        out += b"%010d 00000 n \n" % offset
    out += b"trailer\n<< /Size %d /Root %d 0 R /Info %d 0 R >>\nstartxref\n%d\n%%%%EOF\n" % (
        len(objects) + 1, catalog, info, xref,
    )
    path.write_bytes(bytes(out))


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    write_epub(OUT / "notas-sobre-o-metodo.epub")
    write_pdf(OUT / "caderno-de-exercicios.pdf", [
        ["Caderno de exercícios", "", "Página 1: leia o capítulo 1 e anote as dúvidas."],
        ["Página 2", "", "Explique com suas palavras o que é a teoria de um programa."],
        ["Página 3", "", "Compare duas formas de documentar um sistema."],
    ])
    print("Gerado em", OUT)
