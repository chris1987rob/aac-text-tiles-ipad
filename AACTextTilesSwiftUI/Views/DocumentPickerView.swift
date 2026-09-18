import SwiftUI
import UIKit
import UniformTypeIdentifiers

/// Picks a backup file to restore from.
public struct DocumentPickerView: UIViewControllerRepresentable {
    public let onPicked: (URL) -> Void

    public init(onPicked: @escaping (URL) -> Void) {
        self.onPicked = onPicked
    }

    public func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let types: [UTType] = [.json, .data]
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: types, asCopy: true)
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }

    public func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    public func makeCoordinator() -> Coordinator { Coordinator(self) }

    public class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: DocumentPickerView
        init(_ parent: DocumentPickerView) { self.parent = parent }

        public func documentPicker(_ controller: UIDocumentPickerViewController,
                                   didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            parent.onPicked(url)
        }
    }
}
