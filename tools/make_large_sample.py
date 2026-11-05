"""Gera samples/livro-grande.epub (fora do git) para medir desempenho com livros extensos.

Uso: python tools/make_large_sample.py [capitulos] [kb_por_capitulo]
Padrão: 150 capítulos de 60 KB, cerca de 9 MB de texto, na ordem de um livro técnico muito longo.
O texto é gerado aqui; a primeira ocorrência de "agulha" fica no último capítulo, para testar a busca.
"""

import pathlib
import random
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "samples" / "livro-grande.epub"

WORDS = (
    "programa teoria método leitura forma conteúdo nome conceito fronteira tipo prova exemplo "
    "tabela código texto margem nota capítulo argumento sistema equipe compreensão escolha "
    "pergunta consequência documentação convivência ofício aprendiz estudo memória hábito"
).split()


def paragraph(rng):
    return " ".join(rng.choice(WORDS) for _ in range(rng.randint(40, 90))).capitalize() + "."


def chapter(index, total, size_kb, rng):
    body, size = [], 0
    while size < size_kb * 1024:
        text = paragraph(rng)
        body.append(f"<p>{text}</p>")
        size += len(text.encode("utf-8")) + 7
    if index == total:
        body.append("<p>Aqui, no fim do livro, está a agulha procurada.</p>")
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="pt-BR" lang="pt-BR">
<head><meta charset="utf-8"/><title>Capítulo {index}</title></head>
<body><h1>Capítulo {index}</h1>
{chr(10).join(body)}
</body></html>
"""


def main():
    total = int(sys.argv[1]) if len(sys.argv) > 1 else 150
    size_kb = int(sys.argv[2]) if len(sys.argv) > 2 else 60
    rng = random.Random(42)
    OUT.parent.mkdir(exist_ok=True)
    slugs = [f"cap{i}" for i in range(1, total + 1)]
    manifest = "\n".join(f'<item id="{s}" href="{s}.xhtml" media-type="application/xhtml+xml"/>' for s in slugs)
    spine = "\n".join(f'<itemref idref="{s}"/>' for s in slugs)
    toc = "".join(f'<li><a href="{s}.xhtml">Capítulo {i}</a></li>' for i, s in enumerate(slugs, start=1))
    opf = f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid" xml:lang="pt-BR">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="uid">urn:uuid:marginalia-livro-grande</dc:identifier>
<dc:title>Livro grande de teste</dc:title>
<dc:creator>Marginália</dc:creator>
<dc:language>pt-BR</dc:language>
<meta property="dcterms:modified">2026-11-01T00:00:00Z</meta>
</metadata>
<manifest>
<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
{manifest}
</manifest>
<spine>
{spine}
</spine>
</package>
"""
    nav = f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="pt-BR">
<head><meta charset="utf-8"/><title>Sumário</title></head>
<body><nav epub:type="toc" id="toc"><ol>{toc}</ol></nav></body>
</html>
"""
    with zipfile.ZipFile(OUT, "w") as epub:
        epub.writestr(zipfile.ZipInfo("mimetype"), "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        epub.writestr(
            "META-INF/container.xml",
            '<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">'
            '<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>'
            "</rootfiles></container>",
            compress_type=zipfile.ZIP_DEFLATED,
        )
        epub.writestr("OEBPS/content.opf", opf, compress_type=zipfile.ZIP_DEFLATED)
        epub.writestr("OEBPS/nav.xhtml", nav, compress_type=zipfile.ZIP_DEFLATED)
        for index, slug in enumerate(slugs, start=1):
            epub.writestr(f"OEBPS/{slug}.xhtml", chapter(index, total, size_kb, rng), compress_type=zipfile.ZIP_DEFLATED)
    print("Gerado", OUT, OUT.stat().st_size // 1024, "KB")


if __name__ == "__main__":
    main()
