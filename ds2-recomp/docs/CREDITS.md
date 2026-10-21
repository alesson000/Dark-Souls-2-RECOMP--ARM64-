# 🏆 Créditos e Reconhecimentos — Dark Souls II Android Recomp

Este projeto é desenvolvido sobre uma fundação de bibliotecas e ferramentas open-source pioneiras na emulação e recompilação de consoles de 7ª geração.

---

## 🌟 O Projeto ReXGlue (Coração da Recompilação)

O maior crédito deste projeto pertence aos desenvolvedores e colaboradores do **ReXGlue SDK**.

* **Repositório Oficial:** [ReXGlue no GitHub](https://github.com/)
* **Papel no Projeto:**
  * **Análise Estática de Binários XEX:** Desmontagem e levantamento de fluxo de controle (CFG) do executável de Xbox 360 (`default.xex`).
  * **Gerador de Código AOT C++ (`rexglue codegen`):** Tradução estática das instruções PowerPC de 64 bits para blocos C++ modernos (C++23) com suporte a registradores vetoriais AltiVec/VMX.
  * **Emulação das APIs de Kernel Xenon:** Implementação do subsistema de threads nativas, primitivas de sincronização, chamadas de sistema e alocação de memória virtual do console.
  * **Camada Gráfica Vulkan:** Pipeline de tradução dos comandos de GPU Xenos para comandos Vulkan multiplataforma.

Sem o ferramental criado pelo ReXGlue, a recompilação estática de *Dark Souls II* para plataformas móveis modernas seria inviável.

---

## 🎮 FromSoftware & Bandai Namco

* **FromSoftware, Inc.** — Criadores de *Dark Souls II*, uma obra-prima que desafia e encanta gerações de jogadores. A dedicação da comunidade neste projeto visa celebrar, preservar e expandir a acessibilidade do jogo para novos horizontes tecnológicos.

---

## 🛠️ Bibliotecas e Ferramentas Terceiras

* **[libadrenotools](https://github.com/bylaws/libadrenotools)** por **Bylaws**:
  * Essencial para o ecossistema Android em dispositivos Qualcomm Adreno.
  * Permite carregar drivers Vulkan customizados em tempo de execução (Mesa Turnip) e ajustar os reguladores de frequência da GPU para modo Turbo máximo.

* **[libSDL (Simple DirectMedia Layer) / SDL3](https://github.com/libsdl-org/SDL)**:
  * Gerenciamento de ciclo de vida de aplicativos Android (`SDLActivity`), criação de superfícies gráficas `ANativeWindow`, eventos de toque e controle de áudio de baixa latência.

* **[zFitness / gow3_android](https://github.com/zFitness/gow3_android)**:
  * Agradecimento especial pelo trabalho pioneiro na estrutura, documentação e organização de projetos de recompilação e portes nativos no ecossistema Android.

* **LLVM / Clang Project**:
  * Pelo compilador C++23 de alta performance e otimizações de geração de código para a arquitetura AArch64 (ARMv8-A).

* **Android Open Source Project (AOSP) & Android NDK Team**:
  * Pelas ferramentas de desenvolvimento e suporte a bibliotecas C++ nativas no sistema operacional Android.
