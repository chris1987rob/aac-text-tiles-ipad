import UIKit
import WebKit
import AVFoundation

class ViewController: UIViewController, WKNavigationDelegate, WKScriptMessageHandler, WKUIDelegate {

    private var webView: WKWebView!
    private let speechSynthesizer = AVSpeechSynthesizer()

    override var prefersStatusBarHidden: Bool {
        return true
    }

    override var prefersHomeIndicatorAutoHidden: Bool {
        return true
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.0, green: 0.514, blue: 0.412, alpha: 1.0) // GoTalk Teal

        setupWebView()
        loadLocalApp()
    }

    private func setupWebView() {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        // Enable file access
        config.preferences.setValue(true, forKey: "allowFileAccessFromFileURLs")
        config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")

        // Add native Swift message handler for hardware speech bridge
        let contentController = WKUserContentController()
        contentController.add(self, name: "speakText")
        contentController.add(self, name: "hapticFeedback")
        config.userContentController = contentController

        webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 0.0, green: 0.514, blue: 0.412, alpha: 1.0)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(webView)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func loadLocalApp() {
        guard let wwwPath = Bundle.main.path(forResource: "www", ofType: nil) else {
            // Fallback to bundle root if files are at top level
            if let indexPath = Bundle.main.url(forResource: "index", withExtension: "html") {
                webView.loadFileURL(indexPath, allowingReadAccessTo: Bundle.main.bundleURL)
            }
            return
        }

        let wwwURL = URL(fileURLWithPath: wwwPath)
        let indexURL = wwwURL.appendingPathComponent("index.html")

        if FileManager.default.fileExists(atPath: indexURL.path) {
            webView.loadFileURL(indexURL, allowingReadAccessTo: wwwURL)
        }
    }

    // MARK: - WKScriptMessageHandler (Native Swift Speech & Haptics Bridge)
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "speakText", let text = message.body as? String, !text.isEmpty {
            speakNative(text: text)
        } else if message.name == "hapticFeedback" {
            let impact = UIImpactFeedbackGenerator(style: .medium)
            impact.impactOccurred()
        }
    }

    private func speakNative(text: String) {
        speechSynthesizer.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.95
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0

        if let voice = AVSpeechSynthesisVoice(language: "en-US") {
            utterance.voice = voice
        }

        speechSynthesizer.speak(utterance)
    }

    // MARK: - WKNavigationDelegate
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        print("AAC Text Tiles iPad loaded successfully!")
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        print("WebView navigation error: \(error.localizedDescription)")
    }
}
