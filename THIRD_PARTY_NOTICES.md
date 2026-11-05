# Avisos de terceiros

O código da Marginália é distribuído sob a licença [MIT](LICENSE). Os componentes abaixo têm licenças próprias, que continuam valendo.

## Incluídos neste repositório

| Componente | Licença | Texto |
| --- | --- | --- |
| [Inter](https://github.com/rsms/inter) | SIL Open Font License 1.1 | [OFL-inter.txt](app/src/main/assets/licenses/OFL-inter.txt) |
| [Literata](https://github.com/googlefonts/literata) | SIL Open Font License 1.1 | [OFL-literata.txt](app/src/main/assets/licenses/OFL-literata.txt) |
| [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) | SIL Open Font License 1.1 | [OFL-jetbrainsmono.txt](app/src/main/assets/licenses/OFL-jetbrainsmono.txt) |
| [Material Symbols](https://github.com/google/material-design-icons) (ícones convertidos para vetor) | Apache 2.0 | [APACHE-2.0.txt](app/src/main/assets/licenses/APACHE-2.0.txt) |

As fontes foram reduzidas com `tools/subset_fonts.py`: parte dos alfabetos foi removida e, nas versões da interface, os eixos variáveis foram limitados. Continuam sob a mesma licença OFL.

## Bibliotecas usadas no build

| Componente | Licença |
| --- | --- |
| [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) | BSD-3-Clause ([texto](app/src/main/assets/licenses/BSD-readium.txt)) |
| [PdfiumAndroid](https://github.com/marain87/PdfiumAndroid) e [AndroidPdfViewer](https://github.com/marain87/AndroidPdfViewer) | Apache 2.0 |
| PDFium | BSD-3-Clause e Apache 2.0 |
| AndroidX, Jetpack Compose, Material 3, Room, DataStore, Hilt e Dagger | Apache 2.0 |
| Kotlin e kotlinx | Apache 2.0 |
| Guava, Okio, Timber e Koi | Apache 2.0 |
| jsoup | MIT |

Os livros de teste não fazem parte do repositório.
