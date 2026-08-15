# Physical device test research notes

## Official sources reviewed

- Chrome DevTools: Remote debug Android devices — https://developer.chrome.com/docs/devtools/remote-debugging
  - Enable Android Developer Options and USB Debugging.
  - Use desktop Chrome `chrome://inspect`, enable Discover USB devices, connect the device, authorize the debugging prompt, and inspect the remote Chrome tab.
  - Screencast can affect frame rate and must be disabled during performance measurement.
  - Optional direct CDP route: use `adb devices -l`, forward `9222` to `localabstract:chrome_devtools_remote`, then inspect `/json` targets.

- Apple: Inspecting iOS and iPadOS — https://developer.apple.com/documentation/safari-developer-tools/inspecting-ios
  - On iOS/iPadOS enable Settings > Apps > Safari > Advanced > Web Inspector.
  - Connect device to a Mac, trust the Mac if prompted, and open the page through Safari Develop menu.
  - Wired inspection is preferred for repeatable testing; network inspection can be enabled after an initial cable connection.
  - Simulators expose Web Inspector automatically but are not a substitute for physical GPU/thermal validation.

- WebKit Web Inspector Timelines — https://webkit.org/web-inspector/timelines-tab/
  - Use Timelines recording for network, layout/rendering, JavaScript/events, CPU, memory, and frame-oriented views.
  - Frames view plots time per rendering frame; 30 FPS and 60 FPS guide lines correspond approximately to 33 ms and 16 ms.
  - Export recordings for later comparison.

- Apple Web Inspector overview — https://developer.apple.com/documentation/safari-developer-tools/web-inspector
  - Relevant tabs: Console, Network, Timelines, Graphics, Layers, and Audit.
  - Graphics and Layers are useful for canvas/compositing inspection; Timelines is the primary frame and CPU evidence source.

## Implications for Forest Depths

The physical test must record actual device/browser/GPU identity, DPR, viewport, orientation, thermal state, and renderer. Disable screencast and unnecessary inspector features during timing. Test chapter 01, 05, and 09 because they cover the lightest, middle, and deepest/high-atmosphere states. Capture both normal motion and reduced motion. Record frame-time distribution rather than relying on a single average FPS value.
