# Leitor EPUB/PDF para Android

App Android para **leitura densa de EPUB** (livros técnicos e de filosofia). PDF é suportado, mas é secundário.
Nome do app: **Marginália**. Projeto solo, código fechado, será publicado na Google Play. O PRD completo está em `docs/PRD.md`: leia antes de decisões de escopo.
Marca e telas de referência: `docs/BRAND.md` (parâmetro de comparação de cada entrega).

## Stack (decidida, não trocar sem perguntar)

- Kotlin + Jetpack Compose + Material 3
- Readium Kotlin Toolkit (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-adapter-pdfium`) para EPUB e PDF. Use a versão estável mais recente no Maven Central.
- Room (SQLite) para livros, posições (Readium `Locator`), anotações e coleções
- Hilt para injeção de dependência
- Storage Access Framework para arquivos (nunca pedir `MANAGE_EXTERNAL_STORAGE`)
- minSdk 29 (Android 10), targetSdk e compileSdk = mais recente estável
- Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`)

## Regras de licença

Código fechado e pago: **toda dependência precisa ser permissiva** (BSD, Apache, MIT, OFL).
Nunca adicionar nada GPL/AGPL (ex.: MuPDF, código do Readest/KOReader/Librera). Na dúvida, pergunte.

## Princípios do produto

1. EPUB primeiro; PDF recebe o básico bem feito.
2. Estudo, não só leitura: anotar, voltar, consultar e exportar valem mais que animação.
3. Anotações saem do app em Markdown (compatível com Obsidian).
4. Zero ruído: sem anúncios, sem conta, sem analytics, sem permissão de internet no MVP. 100% offline.
5. Poucos padrões bem escolhidos em vez de muitas opções.

## Temas de leitura (valores fixos)

| Tema | Fundo | Texto | Secundário | Links/destaque |
| --- | --- | --- | --- | --- |
| Claro | #FAFAF7 | #1F1F1F | #6B6B6B | #2F5DA8 |
| Clássico (padrão) | #FBF8F1 | #262321 | #6F6A62 | #7A3B1D |
| Sépia | #F1E4C9 | #3B2E22 | #6E5A44 | #8A4517 |
| Escuro | #121212 | #CFCAC1 | #8F8B84 | #8AB4F8 |

- A interface do app (barras, menus, diálogos) usa as cores do tema de leitura ativo. Sépia nunca abre menu branco.
- Aplique as cores no EPUB via preferências do Readium (`backgroundColor`, `textColor`), não por CSS injetado à mão.
- Imagens: sem filtro nos temas claros; no escuro, brilho ~90%, nunca inverter.
- Cor de marca: terracota #7A3B1D (escuro: #D08A62), só em botão principal, seleção e progresso.

## Direção de arte

"Biblioteca silenciosa": editorial, calmo, tipográfico. Sem gradientes, sem sombras fortes, sem emojis.
- Fonte da interface: Inter. Fonte de leitura padrão: Literata. Código: JetBrains Mono. Todas OFL, embarcadas em `res/font`.
- Leitura padrão: 18 sp, entrelinha 1,5, alinhado à esquerda com hifenização.
- Leitor em tela cheia; um toque no centro mostra/esconde barras. Nada fixo na tela durante a leitura.
- Cantos 12 dp, ícones Material Symbols Outlined. Transições de 150–250 ms, respeitando "remover animações" do sistema.
- Textos da interface em PT-BR (com `strings.xml` em EN também), curtos e diretos.

## Arquitetura

Pacotes por feature: `library/`, `reader/`, `annotations/`, `settings/`, mais `core/` (db, readium, theme, di).
Camadas: UI (Compose) → ViewModel → casos de uso/repositórios → Readium/Room/SAF.
Estado de UI em `StateFlow`; nada de lógica em Composables.

## Convenções

- Kotlin idiomático, coroutines + Flow, sem RxJava.
- Toda posição de leitura é salva como `Locator` serializado, a cada mudança de página e ao sair.
- Testes unitários para repositórios e exportação de Markdown; teste manual de leitura com livros reais em `samples/` (pasta fora do git).
- Commits pequenos, mensagem em português no imperativo.
- Antes de concluir uma tarefa: `./gradlew assembleDebug` e `./gradlew test` precisam passar.

## Roadmap

- **Fase 0 (atual):** protótipo técnico. Abrir EPUB e PDF pelo SAF, ler paginado, trocar os 4 temas, salvar e restaurar a posição. Validar notas de rodapé, blocos de código e tabelas em 5 livros reais.
- **Fase 1:** todos os requisitos P0 do PRD.
- **Fase 2:** sync, TTS, requisitos P1.
