# Third-Party Notices

This repository vendors or redistributes third-party source code and binary artifacts.
The items below remain covered by their upstream licenses in addition to this
repository's own [MIT License](LICENSE).

## MNN

- Upstream: `alibaba/MNN`
- Repository paths:
  - `app/src/main/cpp/mnn`
  - `app/src/main/cpp/include/audio`
  - `app/src/main/jniLibs/arm64-v8a/libMNN.so`
  - `app/src/main/jniLibs/arm64-v8a/libmnnllmapp.so`
- Upstream license: Apache License 2.0

## nlohmann/json

- Upstream: `nlohmann/json`
- Repository path: `app/src/main/cpp/third_party/nlohmann/json.hpp`
- Upstream license: MIT

## utf8cpp

- Upstream: `nemtrif/utfcpp`
- Repository paths:
  - `app/src/main/cpp/mnn_tts/include/piper/utf8.h`
  - `app/src/main/cpp/mnn_tts/include/piper/utf8/*`
- Upstream license: Boost Software License 1.0
- License text: `licenses/utf8cpp.txt`

## uni-algo

- Upstream: `uni-algo/uni-algo`
- Repository path: `app/src/main/cpp/mnn_tts/include/piper/uni_algo.hpp`
- Upstream license: MIT

## libsherpa-mnn-jni.so

- Repository path: `app/src/main/jniLibs/arm64-v8a/libsherpa-mnn-jni.so`
- First imported in local history at commit: `5a189d3ef3940338be603cd5abee27caf3206cf8`
- SHA-256: `4285A62B38C54CA714076BD69007306408C00509568B893EEF3FDBF141CEF5BB`
- Provenance note: retained from the Android MNN-related project lineage referenced in this repository's README.
- Release note: pin the exact upstream source repository and revision before publishing standalone binary releases.
