# 📱 Dispositivos Testados — Dark Souls II Android Recomp

Este documento reúne o registro detalhado de todos os dispositivos móveis testados com o port de recompilação estática de **Dark Souls II** (Xbox 360 para Android ARM64).

---

## 📋 Tabela Geral de Compatibilidade

| Marca / Modelo | SoC / Processador | GPU | Memória RAM | Driver Gráfico | Versão Android | Status Atual | Detalhes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Motorola Moto G75 5G** (`sorap`) | **Qualcomm Snapdragon 6 Gen 3** (SM6475) | Adreno 710 | 8 GB LPDDR4X | • Vulkan Adreno System<br/>• Mesa Turnip (v24.3+) | Android 14 | 🟡 **Boot Concluído com Sucesso** | Áudio low latency ativo, arena de memória 4.8 GB mapeada, swapchain 2388x1080, 58 thunks corrigidos |

---

## 🔬 Relatório Aprofundado: Motorola Moto G75 5G (Snapdragon 6 Gen 3 / Adreno 710)

O **Moto G75 5G** é o dispositivo de bancada primário utilizado durante a arquitetura e testes do projeto.

### 📐 Especificações de Hardware
* **SoC:** Qualcomm Snapdragon 6 Gen 3 (SM6475, litografia de 4nm)
* **CPU:** 8 núcleos
  * 4x ARM Cortex-A78 @ 2.40 GHz (Núcleos de Performance)
  * 4x ARM Cortex-A55 @ 1.80 GHz (Núcleos de Eficiência)
* **GPU:** Qualcomm Adreno 710
* **Memória RAM:** 8 GB LPDDR4X (com ZRAM / swap virtual ativo)
* **Armazenamento:** UFS 2.2
* **Display:** 2388 x 1080 pixels (120 Hz)
* **Sistema Operacional:** Android 14 (Kernel Linux 6.1 aarch64)

### 📊 Desempenho e Logs de Execução

#### 1. Criação de Superfície Vulkan e Janela
```text
I/DarkSouls2Activity: === Starting DarkSouls2Activity native SDL runtime v1.0.10 ===
V/SDL: Manufacturer: motorola
V/SDL: Device: sorap
V/SDL: Model: moto g75 5G
V/SDL: Window size: 2388x1080 | Device size: 2388x1080
I/ReXGlue: [info] [core] SDL video driver: android
```

#### 2. AdrenoTools & Clocks de GPU
O AdrenoTools inicializa sem erros na GPU Adreno 710 e força com sucesso as frequências de clock em modo Turbo:
```text
I/DS2_RECOMP: AdrenoTools: Turbo clocks forced successfully for Adreno 710!
I/DS2_RECOMP: AdrenoTools: Using Qualcomm system Vulkan driver.
I/AdrenoVK-0: Driver Path: /vendor/lib64/hw/vulkan.adreno.so
```

#### 3. Mapeamento de Memória Virtual do Console
```text
I/DS2_NATIVE: CreateFileMappingHandle: memfd_create succeeded (fd=203, size=4831842304)
I/DS2_RECOMP: Selected XEX entrypoint image: game:\Default.xex
I/DS2_NATIVE: app->OnInitialize() returned: SUCCESS
```

#### 4. Motor de Áudio
```text
I/SDL/APP: Low latency audio enabled
```

---

## 📝 Como Submeter um Novo Dispositivo Testado

Se você possui outro modelo de smartphone ou tablet Android (Snapdragon, MediaTek Dimensity, Google Tensor, etc.) e deseja contribuir com a tabela de compatibilidade:

1. Abra uma nova **Issue** no GitHub com o título: `[Device Test] Nome do Dispositivo - SoC`.
2. Inclua as seguintes informações:
   - **Modelo do aparelho** (ex: *POCO F5, Galaxy S23, Xiaomi 13*).
   - **Processador / GPU** (ex: *Snapdragon 7+ Gen 2 / Adreno 725*).
   - **Quantidade de memória RAM**.
   - **Versão do Android**.
   - **Driver utilizado** (Driver do sistema ou Turnip).
   - **Arquivo de log** gerado pelo botão "Salvar Log" do launcher.
