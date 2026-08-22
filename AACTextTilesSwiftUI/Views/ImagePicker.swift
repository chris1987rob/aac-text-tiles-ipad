import SwiftUI
import UIKit

public struct ImagePicker: UIViewControllerRepresentable {
    @Environment(\.presentationMode) private var presentationMode
    public var sourceType: UIImagePickerController.SourceType = .photoLibrary
    public let onImagePicked: (UIImage) -> Void

    public init(sourceType: UIImagePickerController.SourceType = .photoLibrary, onImagePicked: @escaping (UIImage) -> Void) {
        self.sourceType = sourceType
        self.onImagePicked = onImagePicked
    }

    public func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = sourceType
        picker.allowsEditing = false
        return picker
    }

    public func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    public func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    public class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.onImagePicked(image)
            }
            parent.presentationMode.wrappedValue.dismiss()
        }

        public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.presentationMode.wrappedValue.dismiss()
        }
    }
}

/// Identifies one picker request so the picker can be driven by a single
/// `.sheet(item:)`.
///
/// Both picture buttons used to sit on a view carrying an `.actionSheet` and a
/// `.sheet(isPresented:)` at the same time. Choosing a source set the sheet's
/// flag while the action sheet was still dismissing, and SwiftUI drops a
/// presentation requested while another is in flight - so the button appeared
/// dead. One `item:`-driven sheet, set after the dialog has gone, cannot race.
public struct ImagePickerRequest: Identifiable {
    public let id = UUID()
    public let source: UIImagePickerController.SourceType

    public init(source: UIImagePickerController.SourceType) {
        self.source = source
    }

    /// Falls back to the library when the requested source is unavailable,
    /// e.g. camera on a device that has none.
    public static func resolving(_ preferred: UIImagePickerController.SourceType) -> ImagePickerRequest {
        ImagePickerRequest(
            source: UIImagePickerController.isSourceTypeAvailable(preferred) ? preferred : .photoLibrary
        )
    }
}
