# Marca e interface: referência de entrega

Fonte visual: canvas "Marginália — Marca e Telas" (https://claude.ai/artifact/Kr42J9XMSGWM53KzcizNtP).
Cópia das telas em `docs/design/` (arquivos `.dc.html`). Este documento é o parâmetro de comparação:
ao fim de cada fase, cada item marcado aqui precisa bater com o app rodando.

Nome do app: **Marginália**.

## Símbolo

- Página creme sobre quadrado terracota (#7A3B1D), linhas de texto em tinta e uma barra terracota na margem esquerda.
- Variantes: principal, sobre fundo escuro (#1C1C1B / página #CFCAC1 / barra #D08A62) e monocromática (só traço) para ícone temático do Android 13+.
- Ícone adaptativo: página dentro da zona segura de 66 dp.

## Cor

| Token | Claro | Clássico (padrão) | Sépia | Escuro |
| --- | --- | --- | --- | --- |
| `bg` fundo | #FAFAF7 | #FBF8F1 | #F1E4C9 | #121212 |
| `text` texto | #1F1F1F | #262321 | #3B2E22 | #CFCAC1 |
| `secondary` | #6B6B6B | #6F6A62 | #6E5A44 | #8F8B84 |
| `link` links/destaque | #2F5DA8 | #7A3B1D | #8A4517 | #8AB4F8 |
| `line` fio divisor | #E3E3DE | #E6DFD0 | #D9C6A0 | #2C2B29 |
| `selected` item ativo | #ECECE7 | #EDE6D7 | #E3D3B1 | #2A2927 |
| `code` fundo de código | #F0F0EC | #F3EEE3 | #E8D8B8 | #1C1C1B |
| `brand` | #7A3B1D | #7A3B1D | #7A3B1D | #D08A62 |

- `brand` só em: botão principal, seleção de texto (alças em `brand`; fundo `#D08A62` a 40%, igual nos 4 temas) e progresso.
- Barras, menus, folhas e diálogos usam `bg`/`line` do tema ativo. Nenhum menu branco no Sépia.
- Separação por fio de 1 dp; sombra no máximo `0 2 8 rgba(texto, 8%)` em menus flutuantes. Scrim de folha modal: texto a 24%.
- Cores de destaque (temas claros): amarelo #F2DC8C, verde #CFE3B4, azul #C9DBF2, rosa #F2CBC4. No escuro, versões com opacidade a definir na Fase 1.

## Tipografia

| Papel | Fonte | Tamanho/entrelinha | Peso |
| --- | --- | --- | --- |
| Título de tela | Literata | 28/36 | 500 |
| Título de livro | Literata | 20/26 | 500 |
| Corpo de leitura | Literata | 18 sp, 1,5 | 400 |
| Interface | Inter | 15/22 | 400 e 500 |
| Secundário | Inter | 13 e 12 | 400 |
| Rótulo de seção | Inter | 12/16, caixa alta, +0,08 em | 600 |
| Código no texto | JetBrains Mono | 0,78–0,85 em, 1,5 | 400 |

## Forma

- Cantos 12 dp (cartões, botões, folhas, chips); blocos de código 8 dp.
- Área de toque mínima 48 dp (44 dp só em linhas densas de folha).
- Ícones Material Symbols Outlined, 24 dp.
- Transições 150–250 ms; nenhuma com "remover animações" ligado.

## Telas de referência (Fase 0 e 1)

- [x] **Biblioteca**: título em Literata, "Continuar lendo" com progresso e botão principal, lista com capa, autor e estado, chip PDF, navegação inferior (Biblioteca, Anotações, Ajustes).
- [x] **Leitor**: tela cheia sem nada fixo; toque no centro mostra barras. Barra superior: voltar, título e capítulo, buscar, Aa, marcador. Barra inferior: progresso em `brand`, página e tempo no capítulo, sumário, anotações.
- [x] **Sumário**: abas Sumário / Anotações / Marcadores, capítulo atual com fundo `selected`, subseções recuadas, número de página.
- [x] **Aparência**: folha inferior no tema ativo; 4 temas, tamanho A−/A+, fonte (Literata/Inter), entrelinha (Compacta/Padrão/Ampla), restaurar padrão. O texto atrás muda ao vivo.
- [x] **Seleção**: alças e fundo em `brand`; menu flutuante com 4 cores, Nota, Copiar, mais.
- [x] **Nota de rodapé**: abre em folha inferior sem sair da página; "Ir para as notas".
- [x] **Anotações do livro**: filtros Todas/Destaques/Notas, agrupadas por capítulo, trecho com a cor do destaque, nota abaixo, página e data; botão principal "Exportar em Markdown".
- [x] **Exportar**: escolher o que incluir, prévia em Markdown com frontmatter YAML (title, author, tags, exported), salvar via SAF ou compartilhar.
- [x] **Quatro temas**: as barras do leitor em cada tema batem com a tabela de cor.

## Voz

Curta, direta, sem exclamação nem emoji. "Adicionar livro", "Não foi possível abrir este arquivo.", "12 destaques exportados.", "Nenhuma anotação ainda."

## Estado da implementação (Fase 0 + base da Fase 1)

Conferido no emulador (Pixel 7, Android 16) com `samples/` (EPUB de teste, Dom Casmurro, PDF), em debug e em release com R8.

Diferenças conhecidas em relação ao design:

- **Fundo da seleção de texto**: usa um único tom translúcido nos 4 temas (`#D08A62` a 40%), definido por `readiumCssRsProperties`, porque essa configuração fica fixa enquanto o livro está aberto e os temas mudam ao vivo. As alças usam sempre `#7A3B1D`, inclusive no Escuro.
- **Blocos de código**: usam a fonte monoespaçada do próprio livro ou do sistema. JetBrains Mono no texto do livro também exigiria CSS injetado.
- **Número de página**: é a posição do Readium (cerca de 1 KB compactado, uns 2.800 caracteres), não a página impressa do livro.
- **Minutos no capítulo**: só aparecem quando o arquivo do EPUB contém um único capítulo; em EPUBs com vários capítulos por arquivo a estimativa seria enganosa.
- **Nota de rodapé**: a folha mostra "Nota" sem o número, que o Readium não informa.
