# Exportação dos diagramas para PNG

Os diagramas Mermaid do relatório foram extraídos para os ficheiros `.mmd` nesta pasta:

- `arquitetura-camadas.mmd`
- `modelo-er.mmd`
- `diagrama-classes.mmd`
- `fluxo-utilizador.mmd`

## Como gerar PNG

Pré-requisito: `mmdc` (Mermaid CLI).

```bash
cd docs/diagrams
./export_png.sh
```

Isto gera os ficheiros:

- `arquitetura-camadas.png`
- `modelo-er.png`
- `diagrama-classes.png`
- `fluxo-utilizador.png`
