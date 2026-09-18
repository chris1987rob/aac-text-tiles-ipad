import SwiftUI
import UIKit

/// The scene picture control, used by both the New Page Wizard and Page
/// Options. One component rather than two copies: they are the same decision
/// made at two moments, and two copies drift.
///
/// The parent owns the picker sheet. Stacked presentation modifiers on one view
/// silently refuse to present, so a caller that already has a sheet must drive
/// this one through its own single `.sheet(item:)`.
public struct ScenePicturePicker: View {
    @Binding public var image: UIImage?
    @Binding public var data: Data?
    public let onRequestPicker: (ImagePickerRequest) -> Void

    public init(image: Binding<UIImage?>,
                data: Binding<Data?>,
                onRequestPicker: @escaping (ImagePickerRequest) -> Void) {
        self._image = image
        self._data = data
        self.onRequestPicker = onRequestPicker
    }

    private var hasCamera: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    public var body: some View {
        if let img = image {
            preview(img)
        }

        // Always offered, even where no camera answers. Hiding this row on a
        // camera-less device (the simulator) meant the control could never be
        // seen or checked in the one place it can be looked at, and it read as
        // "taking a photo was removed". ImagePickerRequest.resolving falls back
        // to the library, so the row is safe to show everywhere.
        pictureRow(icon: "camera.fill",
                   title: image == nil ? "Take Photo with Camera" : "Retake Photo",
                   subtitle: hasCamera ? nil : "No camera on this device - opens the photo library",
                   tint: "#0284C7") {
            onRequestPicker(ImagePickerRequest.resolving(.camera))
        }

        pictureRow(icon: "photo.on.rectangle.angled",
                   title: image == nil ? "Choose from Photo Library" : "Choose a Different Picture",
                   subtitle: nil,
                   tint: "#008369") {
            onRequestPicker(ImagePickerRequest.resolving(.photoLibrary))
        }

        if image != nil {
            pictureRow(icon: "trash.fill", title: "Remove Picture", subtitle: nil, tint: "#B91C1C") {
                image = nil
                data = nil
            }
        }
    }

    // MARK: - Preview

    /// Shows the picture cropped exactly as the page will crop it. A scene photo
    /// is drawn with `scaledToFill`, so a tall photo loses its top and bottom
    /// and a wide one loses its sides - a small uncropped thumbnail hides the
    /// very thing worth checking before committing to a picture.
    @ViewBuilder
    private func preview(_ img: UIImage) -> some View {
        let aspect = Self.boardAspectRatio
        let height: CGFloat = 190

        VStack(spacing: 8) {
            Image(uiImage: img)
                .resizable()
                .scaledToFill()
                .frame(width: height * aspect, height: height)
                .clipped()
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color(hex: "#CBD5E1"), lineWidth: 1)
                )

            Text("Preview - this is what shows on the page")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Color(hex: "#64748B"))

            Text("\(Int(img.size.width)) x \(Int(img.size.height)) picture")
                .font(.system(size: 11))
                .foregroundColor(Color(hex: "#94A3B8"))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }

    /// The shape of the board area on this device, so the preview crops the way
    /// the real page will. Rotating the iPad changes it, which is honest: the
    /// crop really does change with orientation.
    private static var boardAspectRatio: CGFloat {
        let bounds = UIScreen.main.bounds
        let boardHeight = max(bounds.height - 60, 1)   // minus the navigation bar
        let ratio = bounds.width / boardHeight
        return min(max(ratio, 0.5), 2.2)
    }

    // MARK: - Rows

    @ViewBuilder
    private func pictureRow(icon: String, title: String, subtitle: String?, tint: String, action: @escaping () -> Void) -> some View {
        PictureRow(icon: icon, title: title, subtitle: subtitle, tint: tint, action: action)
    }
}

/// The same picture-source rows, for a button rather than a scene.
///
/// Quick Edit used to ask this with a `.confirmationDialog`. On an iPad that is
/// a popover, and a popover raised from inside a sheet is anchored to the
/// sheet's edge: in landscape it appeared beside the form and was usable, in
/// portrait it was laid out below the form, off the bottom of the screen, and
/// "Add Picture" simply did nothing you could see. Orientation deciding whether
/// a control exists is not a thing to work around - the rows belong in the form,
/// where they are always on screen and always in the same place.
public struct PictureSourceRows: View {
    @Binding public var data: Data?
    public let onRequestPicker: (ImagePickerRequest) -> Void

    public init(data: Binding<Data?>, onRequestPicker: @escaping (ImagePickerRequest) -> Void) {
        self._data = data
        self.onRequestPicker = onRequestPicker
    }

    private var hasCamera: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    public var body: some View {
        PictureRow(icon: "camera.fill",
                   title: data == nil ? "Take Photo with Camera" : "Retake Photo",
                   subtitle: hasCamera ? nil : "No camera on this device - opens the photo library",
                   tint: "#0284C7") {
            onRequestPicker(ImagePickerRequest.resolving(.camera))
        }

        PictureRow(icon: "photo.on.rectangle.angled",
                   title: data == nil ? "Choose from Photo Library" : "Choose a Different Picture",
                   subtitle: nil,
                   tint: "#008369") {
            onRequestPicker(ImagePickerRequest.resolving(.photoLibrary))
        }

        if data != nil {
            PictureRow(icon: "trash.fill", title: "Remove Picture", subtitle: nil, tint: "#B91C1C") {
                data = nil
            }
        }
    }
}

/// One row of the picture control. Shared so the scene picker and the button
/// picker cannot drift apart.
public struct PictureRow: View {
    public let icon: String
    public let title: String
    public let subtitle: String?
    public let tint: String
    public let action: () -> Void

    public init(icon: String, title: String, subtitle: String? = nil, tint: String, action: @escaping () -> Void) {
        self.icon = icon
        self.title = title
        self.subtitle = subtitle
        self.tint = tint
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: tint))
                    .frame(width: 26)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color(hex: "#1E293B"))
                    if let subtitle = subtitle {
                        Text(subtitle)
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: "#94A3B8"))
                    }
                }
                Spacer()
            }
            .contentShape(Rectangle())
            .padding(.vertical, 2)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
