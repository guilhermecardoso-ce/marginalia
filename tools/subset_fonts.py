"""Reduz as fontes OFL embarcadas. Precisa de: pip install fonttools

Uso: python tools/subset_fonts.py <pasta com os TTFs originais do Google Fonts>
Entrada: inter.ttf, inter_italic.ttf, literata.ttf, literata_italic.ttf, jetbrains_mono.ttf

- Leitura (assets/fonts): mantém os eixos de peso e tamanho óptico, corta alfabetos fora do conjunto.
- Interface (res/font): fixa o tamanho óptico e limita o peso aos usados na escala tipográfica.

Grego fica porque livros de filosofia o citam. Cirílico, que nem todas estas fontes trazem, cai na
fonte do sistema. A licença OFL é mantida na tabela `name` e em assets/licenses.
"""

import io
import pathlib
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/font"
ASSETS = ROOT / "app/src/main/assets/fonts"

UNICODES = [
    *range(0x0000, 0x0250),  # Latim e Latim estendido
    0x0259,
    *range(0x0300, 0x0370),  # Diacríticos combinantes
    *range(0x0370, 0x0400),  # Grego
    *range(0x1E00, 0x2000),  # Latim adicional e grego estendido
    *range(0x2000, 0x2070),  # Pontuação
    *range(0x2070, 0x20A0),  # Sobrescritos e subscritos
    *range(0x20A0, 0x20C1),  # Moedas
    *range(0x2100, 0x2150),  # Símbolos de letras
    *range(0x2190, 0x2200),  # Setas
    *range(0x2200, 0x2300),  # Operadores matemáticos
    *range(0xFB00, 0xFB07),  # Ligaduras
]

# (arquivo de origem, destino, limites de eixo; None mantém o eixo inteiro)
JOBS = [
    ("literata.ttf", ASSETS / "Literata.ttf", {}),
    ("literata_italic.ttf", ASSETS / "Literata-Italic.ttf", {}),
    ("inter.ttf", ASSETS / "Inter.ttf", {}),
    ("inter_italic.ttf", ASSETS / "Inter-Italic.ttf", {}),
    ("inter.ttf", RES / "inter.ttf", {"opsz": 14, "wght": (400, 600)}),
    ("literata.ttf", RES / "literata.ttf", {"opsz": 20, "wght": (400, 600)}),
    ("literata_italic.ttf", RES / "literata_italic.ttf", {"opsz": 16, "wght": 400}),
    ("jetbrains_mono.ttf", RES / "jetbrains_mono.ttf", {"wght": (400, 500)}),
]


def main():
    source = pathlib.Path(sys.argv[1])
    total_before = total_after = 0
    for name, target, limits in JOBS:
        font = TTFont(source / name)
        if limits:
            font = instancer.instantiateVariableFont(font, limits)
            # Salva e relê: o subset falha sobre as tabelas do instancer ainda em memória.
            buffer = io.BytesIO()
            font.save(buffer)
            buffer.seek(0)
            font = TTFont(buffer)
        options = subset.Options()
        options.layout_features = ["*"]
        options.name_IDs = ["*"]
        options.name_languages = ["*"]
        options.notdef_outline = True
        options.hinting = False
        subsetter = subset.Subsetter(options)
        subsetter.populate(unicodes=UNICODES)
        subsetter.subset(font)
        font.save(target)

        cmap = font.getBestCmap()
        missing = [hex(c) for c in (0x00E3, 0x00E7, 0x03B1, 0x2014, 0x201C) if c not in cmap]
        before, after = (source / name).stat().st_size, target.stat().st_size
        total_before += before
        total_after += after
        print(f"{target.name:22} {before // 1024:5} -> {after // 1024:5} KB  faltando: {missing or 'nada'}")
    print(f"total {total_before // 1024} -> {total_after // 1024} KB")


if __name__ == "__main__":
    main()
