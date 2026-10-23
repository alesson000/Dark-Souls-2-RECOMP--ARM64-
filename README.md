<div align="center">

# ⚔️ Dark Souls II — Recompilação Estática para Android (ARM64)
### Recompilação Estática Nativa de Xbox 360 para Android AArch64 via ReXGlue SDK & Vulkan

[![Platform](https://img.shields.io/badge/Platform-Android%2011%2B%20(ARM64--v8a)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Recompiler](https://img.shields.io/badge/Recompiler-ReXGlue%20SDK%20v0.10.0-blueviolet?style=for-the-badge&logo=c%2B%2B&logoColor=white)](#-créditos-ao-projeto-de-recompilação-rexglue)
[![Graphics](https://img.shields.io/badge/Graphics-Vulkan%201.3%20%2F%20AdrenoTools-red?style=for-the-badge&logo=vulkan&logoColor=white)](#)
[![Status](https://img.shields.io/badge/Status-WIP%20%2F%20Experimental-orange?style=for-the-badge)](#-status-atual-do-projeto--roadmap)
[![License](https://img.shields.io/badge/License-GPL--3.0%20%2F%20MIT-blue?style=for-the-badge)](#-licença--disclaimer-legal)

<br/>

**[📖 Visão Geral](#-visão-geral)** • **[📱 Dispositivos Testados](#-dispositivos-testados)** • **[🏆 Créditos & ReXGlue](#-créditos-ao-projeto-de-recompilação-rexglue)** • **[🎮 Como Instalar & Jogar](#-como-instalar-e-configurar)** • **[🛠️ Como Compilar](#️-como-compilar-o-código-fonte)** • **[🗺️ Roadmap](#-status-atual-do-projeto--roadmap)** • **[⚖️ Aviso Legal](#-licença--disclaimer-legal)**

---

</div>

> [!WARNING]
> **PROJETO EXPERIMENTAL EM DESENVOLVIMENTO ATIVO (PRE-RELEASE / WIP):**  
> Este repositório contém o código-fonte da recompilação e os scripts do aplicativo Android. O projeto ainda **NÃO possui uma versão final pública** para o público geral, pois o pipeline de renderização e subsistemas gráficos 3D está em fase ativa de depuração e compatibilização de shaders.
>
> ❌ **Nenhum arquivo protegido por direitos autorais ou dados proprietários do jogo está incluído neste repositório ou nas builds.** O usuário deve fornecer seus próprios arquivos legalmente extraídos da sua cópia original de Xbox 360.

---

## 📖 Visão Geral

Este projeto consiste na **recompilação estática antecipada (AOT - Ahead-Of-Time Recompilation)** do executável `default.xex` de **Dark Souls II** (Xbox 360, Title ID `465307E4`, versão `0.0.0.8`) para código binário nativo **ARM64-v8a (AArch64 C++23)**.

Ao contrário de emuladores convencionais (que realizam tradução dinâmica JIT instruction-by-instruction consumindo alto poder de CPU e gerando gargalos térmicos), a recompilação estática converte todo o código PowerPC da arquitetura Xenon diretamente em funções C++ compiladas com otimizações nativas Clang/LLVM.

### 🌟 Destaques da Arquitetura:
* **Tradução Direta PowerPC ➡️ ARM64:** Todo o conjunto de instruções PowerPC 64-bit do jogo executa como código nativo de máquina, sem intérprete intermediário.
* **Backend Gráfico Vulkan Nativo:** Integração com o pipeline Vulkan 1.3 do ReXGlue com suporte a superfícies `ANativeWindow` do Android.
* **AdrenoTools Integrado:** Suporte a GPU Turbo Clocks automáticos e carregamento dinâmico de drivers customizados **Mesa Turnip** para chipsets Qualcomm Snapdragon.
* **Memória Mapeada com `memfd_create`:** Arena de memória virtual de 4.8 GB mapeada em baixo nível para simular o layout de memória do console original com zero-copy.
* **Launcher Nativo Integrado:** Interface Android nativa com seletor de diretórios, carregamento de drivers, alternador de Turbo Clock e visualizador de logs em tempo real.

---

## 📱 Dispositivos Testados

Esta seção documenta o comportamento, desempenho e logs coletados em hardware real Android.

### 📊 Matriz de Compatibilidade de Hardware

| Dispositivo | SoC / Processador | GPU / Arquitetura | Memória RAM | Driver Vulkan Testado | Resolução de Swapchain | Status Atual |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Motorola Moto G75 5G**  | **Qualcomm Snapdragon 6 Gen 3** | **Adreno 710** | 8 GB  | • System Driver: `/vendor/lib64/hw/vulkan.adreno.so`<br/>• Mesa Turnip (v24.3+) | 2388x1080 (Nativa) | 🟡 **Inicializa com Sucesso** (Áudio ativo, arena mapeada, 58 thunks corrigidos, aguardando pipeline 3D) |
| **Samsung Galaxy M55 5G**  | **Qualcomm Snapdragon 7 Gen 1** | **Adreno 644** | 8 GB  | • System Driver: `/vendor/lib64/hw/vulkan.adreno.so`<br/>• Mesa Turnip (v24.3+) | 2388x1080 (Nativa) | 🟡 **Inicializa com Sucesso** (Áudio ativo, arena mapeada, 58 thunks corrigidos, aguardando pipeline 3D) |
---

#### Marcos de Execução Validados no Dispositivo:
1. **Inicialização do Runtime Nativo & SDL3:**
   - Sucesso no ciclo de vida `onCreate()`, `onStart()`, `onResume()` e criação da superfície `surfaceCreated()`.
   - Inicialização correta de `SDLWindowedAppContext` e vinculação de threads nativas.
2. **Gerenciamento de Memória Virtual:**
   - Criação da arena de 4.8 GB via descritor `memfd_create (fd=203, size=4831842304)`.
3. **Controle de Frequência de GPU (AdrenoTools):**
   - Forçamento bem-sucedido de Turbo Clocks na Adreno 710 via `adrenotools_set_turbo(true)`.
   - Carregamento transparente do driver Vulkan do sistema e compatibilidade com drivers customizados Mesa Turnip via hooks nativos.
4. **Motor de Áudio Low Latency:**
   - Backend de áudio inicializado com sucesso: `I/SDL/APP: Low latency audio enabled`.
5. **Resolução de VTables Secundárias e Adjuster Thunks:**
   - Resolução das 58 rotinas virtuais de subsistemas (Áudio, Cena, Recursos e Gerenciadores) que anteriormente causavam bypass trap (`r3 = 0`).

> [!TIP]
> Deseja testar em outro modelo de smartphone? Quando as builds de teste forem abertas, você poderá submeter seu relatório criando uma [Issue no repositório](https://github.com/) com os logs do Logcat e especificações do seu aparelho!

---

## 🏆 Créditos ao Projeto de Recompilação (ReXGlue)

Este projeto só é possível graças ao esforço extraordinário e à engenharia do ecossistema de código aberto de emulação e recompilação estática de Xbox 360.

### 🥇 Destaque Especial: ReXGlue SDK
* **[ReXGlue Project](https://github.com/)** — O coração da recompilação estática do Xbox 360.
  - Desenvolvido para transformar binários XEX de Xbox 360 em bibliotecas nativas C++ de altíssimo desempenho.
  - Responsável pelo motor de análise de fluxo de controle, gerador de código C++ (`rexglue codegen`), emulação das APIs do kernel Xenon, despacho de chamadas indiretas e tradução de registradores PowerPC.
  - Nossos mais profundos agradecimentos a toda a equipe e mantenedores do projeto **ReXGlue** por fornecer a infraestrutura que viabilizou este porte nativo.

### 🌟 Outros Projetos e Agradecimentos Essenciais:
* **[FromSoftware](https://www.fromsoftware.jp/)** — Desenvolvedores do lendário *Dark Souls II*.
* **[Bylaws / AdrenoTools](https://github.com/bylaws/libadrenotools)** — Ferramenta essencial que viabilizou a substituição de drivers Vulkan (Mesa Turnip) e ativação de Turbo Clocks em GPUs Adreno no Android.
* **[libSDL / SDL3](https://github.com/libsdl-org/SDL)** — Criação de janelas, superfícies Vulkan e gerenciamento de entrada multiplataforma para o ecossistema Android.
* **Equipe LLVM & Clang** — Pelo suporte a compiladores de ponta e geração de código vetorial AArch64 otimizado.

---

## 🎮 Como Instalar e Configurar

> [!IMPORTANT]
> O aplicativo **não funciona** sem os arquivos de dados do jogo. Você precisará extrair os dados da sua cópia legítima de *Dark Souls II* do Xbox 360.

### 1. Obtenha os Arquivos do Jogo
Extraia os arquivos do seu disco/ISO de *Dark Souls II* (Xbox 360):
* `default.xex` (Binário executável do jogo)
* `GameData1.bdt` e `GameData1.bhd`
* `GameData2.bdt` e `GameData2.bhd`
* Pastas de recursos: `sound/`, `movie/`, etc.

### 2. Copie para o Armazenamento do Android
No armazenamento interno do seu celular (onde ficam as pastas `Download`, `Documents`, etc.), crie o diretório:
```text
/sdcard/DarkSouls2/
```
*(Caminho equivalente no sistema: `/storage/emulated/0/DarkSouls2/`)*

Copie todos os arquivos do jogo para dentro desta pasta, garantindo a estrutura:
```text
/sdcard/DarkSouls2/
  ├── default.xex
  ├── GameData1.bdt
  ├── GameData1.bhd
  ├── GameData2.bdt
  ├── GameData2.bhd
  ├── movie/
  └── sound/
```

### 3. Instale o APK
Transfira o arquivo APK gerado para o celular e instale-o.  
Ao abrir pela primeira vez, conceda a permissão de **Acesso a Todos os Arquivos (MANAGE_EXTERNAL_STORAGE)** para que o jogo consiga ler os arquivos `.bdt` volumosos diretamente da memória de alta velocidade.

---

## 🛠️ Como Compilar o Código-Fonte

Caso queira clonar o repositório e compilar o projeto em seu próprio ambiente:

### Pré-requisitos
* **Sistema:** Windows 10/11 ou Linux x86_64
* **ReXGlue SDK:** v0.10.0+
* **Android NDK:** r27c (27.2.12479018)
* **Android SDK:** Build-Tools 34.0.0+ / Platform API 34
* **Compilador Nativo:** Clang 18+, CMake 3.31+, Ninja 1.12+
* **JDK:** Java OpenJDK 17

### Passo 1: Recompilar o Executável XEX (ReXGlue Codegen)
```powershell
# Executar a análise de fluxo e emissão de código C++
rexglue.exe codegen --ignore-stamp ds2_manifest.toml
```

### Passo 2: Compilar a Biblioteca Nativa Android (`libds2.so`)
```powershell
# Configurar variáveis de ambiente do NDK e Clang
. .\tools\env.ps1

# Compilar via Ninja
ninja -C build-android ds2
```

### Passo 3: Gerar o Pacote APK via Gradle
```powershell
cd ds2-android
.\gradlew.bat assembleDebug --no-daemon
```
O APK final será gerado em:  
`ds2-android/app/build/outputs/apk/debug/app-debug.apk`

---

## 🗺️ Status Atual do Projeto & Roadmap

- [x] Extração e descompressão estática do executável `default.xex`.
- [x] Geração e mapeamento de código C++ das funções via ReXGlue SDK.
- [x] Portabilidade das bibliotecas de suporte para a Bionic libc do Android.
- [x] Criação de arena de memória virtual de 4.8 GB com `memfd_create`.
- [x] Integração da camada gráfica Vulkan 1.3 nativa via SDL3.
- [x] Integração da biblioteca `libadrenotools` com Turbo Clocks para Adreno 710.
- [x] Inicialização do subsistema de áudio (`Low latency audio enabled`).
- [x] Resolução de 58 adjuster thunks e métodos virtuais de 5 clusters essenciais.
- [x] Mapeamento e suporte a controles touch overlay na tela e joysticks Bluetooth/USB.
- [ ] Conclusão do loop de renderização 3D e rasterização de shaders no Vulkan.
- [ ] UI Rework
- [ ] GPUs Mali (exynos, unisoc, mediatek)
- [ ] Save Game
- [ ] Otimizações de frametime e sincronização de apresentação de quadros.

---

## ⚖️ Licença & Disclaimer Legal

* **Isenção de Responsabilidade:** Este projeto é um trabalho de pesquisa e engenharia reversa independente conduzido para fins puramente educacionais e de preservação histórica de software.
* **Propriedade Intelectual:** *Dark Souls* e *Dark Souls II* são marcas registradas da **FromSoftware, Inc.** e **Bandai Namco Entertainment**. *Xbox 360* é marca registrada da **Microsoft Corporation**. Este projeto não possui afiliação, patrocínio ou endosso por parte de nenhuma dessas empresas.
* **Arquivos do Jogo:** Nenhuma ROM, ISO, executável do jogo, arquivo de textura, modelo 3D, áudio ou qualquer outro material protegido por copyright é distribuído neste repositório. O usuário é o único responsável por prover cópias legalmente obtidas dos seus próprios jogos.
