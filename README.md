<h1 align="center">
  <a href="https://github.com/Enchograph/HuaJuan/releases">
    <img src="docs/assets/images/icon.jpg" width="150" height="150" alt="HuaJuan icon" /><br>
  </a>
</h1>
<p align="center">
  English | <a href="docs/zh/README.md">中文</a><br>
</p>

<div align="center">

[![][deepwiki-shield]][deepwiki-link]
[![][github-release-shield]][github-release-link]
[![][github-contributors-shield]][github-contributors-link]
[![][license-shield]][license-link]
[![][sponsor-shield]][sponsor-link]

</div>

<div align="center">

# HuaJuan (花卷)

An open-source Android AI client built to bring local models, cloud models, knowledge workflows, and creative tools into one polished mobile experience.

**Out of the box, customizable, and designed for everyday AI workflows on Android.**

❤️ Like HuaJuan? Leave a star! ✨

</div>

## ✨ Why HuaJuan

HuaJuan is more than a chat interface. It is a comprehensive Android AI client that combines local inference, cloud connectivity, knowledge management, image generation, and file workflows in a single app.

### Key Points

- **📱 Modern mobile experience**: A clean interactive UI with rich motion and thoughtful light/dark theme support.
- **🧠 Local and cloud models together**: Run local LLM inference on Android through MNN, with Qwen3-0.6B included for immediate trial, while also connecting to providers such as OpenAI, Gemini, Anthropic, and SiliconFlow.
- **📚 Knowledge-ready workflow**: Build a knowledge base from files, notes, and web pages with your own embedding model choices.
- **🎨 Creative and productive tooling**: Generate images, manage files, translate content, organize topics, and work with customizable assistants in one place.

## 🖼️ Screenshots

<p align="center">
  <img src="docs/assets/images/New%20Chat.jpg" width="32%" alt="New Chat" />
  <img src="docs/assets/images/Docker.jpg" width="32%" alt="Docker" />
  <img src="docs/assets/images/Translate.jpg" width="32%" alt="Translate" />
</p>
<p align="center">
  <img src="docs/assets/images/Knowledge%20Base.jpg" width="32%" alt="Knowledge Base" />
  <img src="docs/assets/images/Image%20Genarating.jpg" width="32%" alt="Image Generating" />
  <img src="docs/assets/images/documents.jpg" width="32%" alt="Documents" />
</p>

## 🌟 Features

### 📱 Mobile Experience

- **Modern interactive UI**: Built with a modern flat visual style and rich animations for a polished experience.
- **Elegant theme switching**: Supports beautiful light and dark theme transitions.
- **Out-of-the-box usability**: Includes a ready-to-try local model experience.

### 🧠 Models and Intelligence

- **Local model deployment on Android**: Uses MNN to enable local LLM deployment and inference on Android devices.
- **Cloud model support**: Connects to a broad range of cloud AI services including OpenAI, Gemini, Anthropic, SiliconFlow, and more.
- **Smart assistants and chat**: Provides a flexible assistant ecosystem with extensive customization options.

### 📚 Knowledge and Creation

- **Knowledge base system**: Index files, notes, and web pages with your preferred embedding models.
- **Professional image generation**: Includes a mobile-first image workflow with detailed adjustable parameters.
- **AI-driven translation**: Supports translation as part of the wider productivity workflow.

### 🗂️ Workflow and Organization

- **Comprehensive file management**: Search, manage, and batch-handle in-app files from a centralized file center.
- **Global search**: Quickly locate content and resources across the app.
- **Topic management**: Keep conversations and materials organized with topic-based structure.

## 🛣️ Development Plan

Current and planned directions for HuaJuan:

- Improvement of interaction logic
- Performance optimization
- **Backend integration of MCP features**
- OCR (Optical Character Recognition), planned around Deepseek OCR
- TTS (Text-to-Speech) interaction, planned around MNN TTS
- Development of a local model market

## 🤝 Contributing

Contributions of all sizes are welcome. You can help HuaJuan by:

- Suggesting improvements
- Reporting bugs and issues
- Optimizing code
- Developing planned features
- Writing project documentation
- Helping with project internationalization (i18n)
- Promoting and sharing HuaJuan

### 🚀 Getting Started

1. **Fork the repository** and clone it locally.
2. **Create a branch** for your work.
3. **Commit and push** your changes.
4. **Open a pull request** with a clear description of what changed and why.

## 🛠️ Build and Run

### 📦 Installation

#### 1. Clone the project

```bash
git clone https://github.com/Enchograph/HuaJuan.git
cd HuaJuan
```

#### 2. Open with Android Studio

- Launch Android Studio
- Select `File` -> `Open`
- Choose the project root directory
- Wait for Gradle sync to complete

#### 3. Configure NDK and CMake

Set the following in `local.properties` if your IDE does not configure them automatically:

```properties
ndk.dir=/path/to/ndk
cmake.dir=/path/to/cmake
```

### ⚙️ Local Build

#### Windows

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

#### macOS/Linux

```bash
./gradlew :app:assembleDebug --no-daemon
```

## 🙏 Acknowledgements

- [MNN](https://github.com/alibaba/MNN): Provides the foundation for HuaJuan's Android local model deployment feature.
- [MnnLlmChat](https://github.com/longluo/MnnLlmChat): Referenced during the implementation of Android local model deployment.

Thanks to the open-source community for the work that makes projects like HuaJuan possible.

## 👥 Contributors

<a href="https://github.com/Enchograph/HuaJuan/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Enchograph/HuaJuan" />
</a>

<br /><br />

<!-- Links & Images -->

[deepwiki-shield]: https://img.shields.io/badge/Deepwiki-HuaJuan-0088CC
[deepwiki-link]: https://deepwiki.com/Enchograph/HuaJuan

[github-release-shield]: https://img.shields.io/github/v/release/Enchograph/HuaJuan
[github-release-link]: https://github.com/Enchograph/HuaJuan/releases
[github-contributors-shield]: https://img.shields.io/github/contributors/Enchograph/HuaJuan
[github-contributors-link]: https://github.com/Enchograph/HuaJuan/graphs/contributors

[license-shield]: https://img.shields.io/badge/License-MIT-yellow.svg?logo=opensourceinitiative
[license-link]: LICENSE
[sponsor-shield]: https://img.shields.io/badge/Sponsor_Support-FF6699.svg?logo=githubsponsors&logoColor=white
[sponsor-link]: docs/assets/images/sponsor.png
