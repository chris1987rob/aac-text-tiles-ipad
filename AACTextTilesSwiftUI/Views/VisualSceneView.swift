import SwiftUI

public struct VisualSceneView: View {
    @ObservedObject public var store: AACStore
    public let onSelectHotspot: (HotspotModel) -> Void
    public let onAddHotspot: () -> Void

    @State private var activeHotspotId: Int? = nil
    @State private var isShowingSourceDialog: Bool = false
    @State private var isShowingImagePicker: Bool = false
    @State private var pickerSourceType: UIImagePickerController.SourceType = .photoLibrary

    public var body: some View {
        let p = store.currentPage

        ZStack {
            // Background Image or SVG scene
            sceneBackgroundView(p)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()

            // Hotspots Layer
            GeometryReader { geo in
                ForEach(p.hotspots) { spot in
                    let xPos = (spot.x / 100.0) * geo.size.width
                    let yPos = (spot.y / 100.0) * geo.size.height
                    let width = (spot.w / 100.0) * geo.size.width
                    let height = (spot.h / 100.0) * geo.size.height
                    let isActive = activeHotspotId == spot.id

                    Button(action: {
                        if store.isEditMode {
                            onSelectHotspot(spot)
                        } else {
                            triggerHotspotSpeech(spot)
                        }
                    }) {
                        ZStack {
                            if store.isEditMode {
                                // Editor Mode Hotspot
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color(hex: "#3B82F6").opacity(0.35))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(Color(hex: "#1D4ED8"), style: StrokeStyle(lineWidth: 2, dash: [4]))
                                    )
                                Text(spot.label)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .background(Color.black.opacity(0.75))
                                    .cornerRadius(6)
                            } else {
                                // Player Mode Hotspot
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(isActive ? Color(hex: "#00E676").opacity(0.4) : Color.clear)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(isActive ? Color(hex: "#00E676") : (spot.style == .highlight ? Color(hex: "#FFEB3B").opacity(0.6) : Color.clear), lineWidth: isActive ? 4 : 2)
                                    )
                            }
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    .frame(width: width, height: height)
                    .position(x: xPos + width / 2, y: yPos + height / 2)
                }
            }

            // Top Toolbar in Editor Mode
            if store.isEditMode {
                VStack {
                    HStack(spacing: 12) {
                        Spacer()

                        // 1. Upload Picture / Change Photo Button
                        Button(action: {
                            isShowingSourceDialog = true
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "photo.on.rectangle.angled")
                                    .font(.system(size: 16, weight: .bold))
                                Text("Upload Picture")
                                    .font(.system(size: 14, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background(Color(hex: "#0284C7"))
                            .cornerRadius(10)
                            .shadow(color: Color.black.opacity(0.15), radius: 4, y: 2)
                        }

                        // 2. Add Hotspot Button
                        Button(action: onAddHotspot) {
                            HStack(spacing: 6) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 16, weight: .bold))
                                Text("Add Hotspot")
                                    .font(.system(size: 14, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background(Color(hex: "#008369"))
                            .cornerRadius(10)
                            .shadow(color: Color.black.opacity(0.15), radius: 4, y: 2)
                        }
                    }
                    .padding(14)
                    Spacer()
                }
            }
        }
        .actionSheet(isPresented: $isShowingSourceDialog) {
            ActionSheet(
                title: Text("Scene Background Picture"),
                message: Text("Upload a photo to use for this visual scene page"),
                buttons: [
                    .default(Text("📷 Take Photo with Camera")) {
                        if UIImagePickerController.isSourceTypeAvailable(.camera) {
                            pickerSourceType = .camera
                            isShowingImagePicker = true
                        } else {
                            pickerSourceType = .photoLibrary
                            isShowingImagePicker = true
                        }
                    },
                    .default(Text("🖼️ Choose from Photo Library")) {
                        pickerSourceType = .photoLibrary
                        isShowingImagePicker = true
                    },
                    .destructive(Text("Reset to Preset Scene")) {
                        store.currentPage.sceneImageData = nil
                        store.save()
                    },
                    .cancel()
                ]
            )
        }
        .sheet(isPresented: $isShowingImagePicker) {
            ImagePicker(sourceType: pickerSourceType) { img in
                if let data = img.jpegData(compressionQuality: 0.85) {
                    store.currentPage.sceneImageData = data
                    store.save()
                }
            }
        }
    }

    private func triggerHotspotSpeech(_ spot: HotspotModel) {
        withAnimation(.easeInOut(duration: 0.15)) {
            activeHotspotId = spot.id
        }
        if store.currentPage.express {
            store.addExpressChip(spot.label.isEmpty ? spot.tts : spot.label)
        }
        if let audioData = spot.audioData {
            SpeechManager.shared.playAudioData(audioData)
        } else {
            SpeechManager.shared.speak(spot.tts.isEmpty ? spot.label : spot.tts)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            activeHotspotId = nil
        }
    }

    @ViewBuilder
    private func sceneBackgroundView(_ p: PageModel) -> some View {
        if let data = p.sceneImageData, let img = UIImage(data: data) {
            Image(uiImage: img)
                .resizable()
                .scaledToFill()
        } else {
            // Built-in Living Room Vector Scene
            ZStack {
                LinearGradient(
                    gradient: Gradient(colors: [Color(hex: "#DBEAFE"), Color(hex: "#BFDBFE")]),
                    startPoint: .top,
                    endPoint: .center
                )
                VStack {
                    Spacer()
                    Rectangle()
                        .fill(LinearGradient(gradient: Gradient(colors: [Color(hex: "#B45309"), Color(hex: "#78350F")]), startPoint: .top, endPoint: .bottom))
                        .frame(height: 180)
                }
                VStack(spacing: 12) {
                    Text("Living Room Visual Scene Display")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Color(hex: "#1E3A8A"))
                    HStack(spacing: 40) {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color(hex: "#3B82F6"))
                            .frame(width: 320, height: 160)
                            .overlay(Text("🛋️ Sofa / Couch").font(.headline).foregroundColor(.white))
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color(hex: "#1E293B"))
                            .frame(width: 220, height: 140)
                            .overlay(Text("📺 Television").font(.headline).foregroundColor(.white))
                    }
                }
            }
        }
    }
}
