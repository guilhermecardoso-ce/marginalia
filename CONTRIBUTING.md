# Como contribuir

Relatos de problemas e pull requests são bem-vindos.

## Relatar um problema

Abra uma issue dizendo o modelo do aparelho, a versão do Android e o que aconteceu. Se o problema ocorre com um livro específico, diga qual; não anexe livros protegidos por direitos autorais.

## Enviar código

1. Rode `./gradlew assembleDebug` e `./gradlew test`. Os dois precisam passar.
2. Faça commits pequenos, com a mensagem em português e no imperativo.
3. Em mudanças visuais, compare com [docs/BRAND.md](docs/BRAND.md) e inclua uma captura de tela.

## Regras do projeto

- **Licenças:** só dependências permissivas (BSD, Apache, MIT, OFL). Nada de GPL ou AGPL, para que o projeto continue reutilizável por qualquer pessoa.
- **Privacidade:** o app funciona 100% offline, sem conta, sem anúncios e sem analytics. Mudanças não podem adicionar permissão de internet.
- **Arquivos:** sempre pelo Storage Access Framework; nunca `MANAGE_EXTERNAL_STORAGE`.
- **Temas:** as cores são fixas e estão em [docs/BRAND.md](docs/BRAND.md).

Mais detalhes de arquitetura e convenções estão em [CLAUDE.md](CLAUDE.md).

Ao contribuir, você concorda que sua contribuição seja distribuída sob a licença [MIT](LICENSE).
