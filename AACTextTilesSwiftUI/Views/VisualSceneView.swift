import SwiftUI

public struct VisualSceneView: View {
    @ObservedObject public var store: AACStore
    public let onSelectHotspot: (HotspotModel) -> Void
    public let onAddHotspot: () -> Void

    @State private var activeHotspotId: Int? = nil
    @State private var isShowingSourceDialog: Bool = false
    @State private var isShowingImagePicker: Bool = false
    @State private var pickerSourceType: UIImagePickerController.SourceType = .photoLibrary

    // Gesture bookkeeping: original percent geometry captured at gesture start
    @State private var dragOrigin: [Int: CGPoint] = [:]
    @State private var resizeOrigin: [Int: CGSize] = [:]
    @State private var movingHotspotId: Int? = nil

    private let minSizePct: Double = 6.0

    public var body: some View {
        let p = store.currentPage

        ZStack {
            sceneBackgroundView(p)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()

            // Hotspots Layer
            GeometryReader { geo in
                ForEach(p.hotspots) { spot in
                    let width = (spot.w / 100.0) * geo.size.width
                    let height = (spot.h / 100.0) * geo.size.height
                    let xPos = (spot.x / 100.0) * geo.size.width + width / 2
                    let yPos = (spot.y / 100.0) * geo.size.height + height / 2

                    hotspotBody(spot: spot, canvas: geo.size)
                        .frame(width: max(width, 1), height: max(height, 1))
                        .position(x: xPos, y: yPos)
                }
            }

            // Top Toolbar in Editor Mode
            if store.isEditMode {
                VStack {
                    HStack(spacing: 12) {
                        Spacer()

                        Button(action: { isShowingSourceDialog = true }) {
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
                        pickerSourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
                        isShowingImagePicker = true
                    },
                    .default(Text("🖼️ Choose from Photo Library")) {
                        pickerSourceType = .photoLibrary
                        isShowingImagePicker = true
                    },
                    .destructive(Text("Reset to Preset Scene")) {
                        var page = store.currentPage
                        page.sceneImageData = nil
                        store.currentPage = page
                        store.save()
                    },
                    .cancel()
                ]
            )
        }
        .sheet(isPresented: $isShowingImagePicker) {
            ImagePicker(sourceType: pickerSourceType) { img in
                let sized = img.downscaled(maxDimension: 2048)
                if let data = sized.jpegData(compressionQuality: 0.85) {
                    var page = store.currentPage
                    page.sceneImageData = data
                    store.currentPage = page
                    store.save()
                }
            }
        }
    }

    // MARK: - Hotspot rendering

    @ViewBuilder
    private func hotspotBody(spot: HotspotModel, canvas: CGSize) -> some View {
        let isActive = activeHotspotId == spot.id
        let isMoving = movingHotspotId == spot.id

        ZStack {
            if store.isEditMode {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(hex: "#3B82F6").opacity(isMoving ? 0.55 : 0.35))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(hex: "#1D4ED8"),
                                    style: StrokeStyle(lineWidth: isMoving ? 3 : 2, dash: [4]))
                    )

                HStack(spacing: 4) {
                    Image(systemName: "arrow.up.and.down.and.arrow.left.and.right")
                        .font(.system(size: 11, weight: .bold))
                    Text(spot.label)
                        .font(.system(size: 13, weight: .bold))
                        .lineLimit(1)
                }
                .foregroundColor(.white)
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(Color.black.opacity(0.75))
                .cornerRadius(6)
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(isActive ? Color(hex: "#00E676").opacity(0.4) : Color.clear)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(isActive ? Color(hex: "#00E676")
                                             : (spot.style == .highlight ? Color(hex: "#FFEB3B").opacity(0.6) : Color.clear),
                                    lineWidth: isActive ? 4 : 2)
                    )
                if spot.style == .outline {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.white.opacity(0.85), style: StrokeStyle(lineWidth: 2, dash: [6]))
                }
            }
        }
        // CRITICAL: makes fully transparent hotspots hit-testable in Player mode
        .contentShape(Rectangle())
        .overlay(resizeHandle(spot: spot, canvas: canvas), alignment: .bottomTrailing)
        .onTapGesture {
            if store.isEditMode {
                onSelectHotspot(spot)
            } else {
                triggerHotspotSpeech(spot)
            }
        }
        .gesture(moveGesture(spot: spot, canvas: canvas))
    }

    @ViewBuilder
    private func resizeHandle(spot: HotspotModel, canvas: CGSize) -> some View {
        if store.isEditMode {
            Circle()
                .fill(Color.white)
                .overlay(Circle().stroke(Color(hex: "#1D4ED8"), lineWidth: 3))
                .overlay(
                    Image(systemName: "arrow.down.right")
                        .font(.system(size: 11, weight: .black))
                        .foregroundColor(Color(hex: "#1D4ED8"))
                )
                .frame(width: 34, height: 34)
                .offset(x: 12, y: 12)
                .contentShape(Rectangle())
                .gesture(resizeGesture(spot: spot, canvas: canvas))
        }
    }

    // MARK: - Gestures

    private func moveGesture(spot: HotspotModel, canvas: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 4)
            .onChanged { value in
                guard store.isEditMode, canvas.width > 0, canvas.height > 0 else { return }
                if dragOrigin[spot.id] == nil {
                    dragOrigin[spot.id] = CGPoint(x: spot.x, y: spot.y)
                    movingHotspotId = spot.id
                }
                guard let origin = dragOrigin[spot.id] else { return }
                let dx = (value.translation.width / canvas.width) * 100.0
                let dy = (value.translation.height / canvas.height) * 100.0
                updateHotspot(id: spot.id) { h in
                    h.x = clampPct(Double(origin.x) + dx, upper: 100.0 - h.w)
                    h.y = clampPct(Double(origin.y) + dy, upper: 100.0 - h.h)
                }
            }
            .onEnded { _ in
                dragOrigin[spot.id] = nil
                movingHotspotId = nil
                store.save()
            }
    }

    private func resizeGesture(spot: HotspotModel, canvas: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 2)
            .onChanged { value in
                guard store.isEditMode, canvas.width > 0, canvas.height > 0 else { return }
                if resizeOrigin[spot.id] == nil {
                    resizeOrigin[spot.id] = CGSize(width: spot.w, height: spot.h)
                    movingHotspotId = spot.id
                }
                guard let origin = resizeOrigin[spot.id] else { return }
                let dw = (value.translation.width / canvas.width) * 100.0
                let dh = (value.translation.height / canvas.height) * 100.0
                updateHotspot(id: spot.id) { h in
                    h.w = min(max(Double(origin.width) + dw, minSizePct), 100.0 - h.x)
                    h.h = min(max(Double(origin.height) + dh, minSizePct), 100.0 - h.y)
                }
            }
            .onEnded { _ in
                resizeOrigin[spot.id] = nil
                movingHotspotId = nil
                store.save()
            }
    }

    private func clampPct(_ value: Double, upper: Double) -> Double {
        return min(max(value, 0.0), max(upper, 0.0))
    }

    private func updateHotspot(id: Int, _ mutate: (inout HotspotModel) -> Void) {
        var page = store.currentPage
        guard let idx = page.hotspots.firstIndex(where: { $0.id == id }) else { return }
        mutate(&page.hotspots[idx])
        store.currentPage = page
    }

    // MARK: - Speech

    private func triggerHotspotSpeech(_ spot: HotspotModel) {
        withAnimation(.easeInOut(duration: 0.15)) {
            activeHotspotId = spot.id
        }
        if store.currentPage.express {
            store.addExpressChip(spot.label.isEmpty ? spot.tts : spot.label)
        }

        switch spot.action {
        case .recorded:
            if let audioData = spot.audioData {
                SpeechManager.shared.playAudioData(audioData)
            } else {
                SpeechManager.shared.speak(spot.tts.isEmpty ? spot.label : spot.tts)
            }
        case .jump:
            if let target = spot.jumpPageId,
               let idx = store.pages.firstIndex(where: { $0.id == target }) {
                store.currentPageIndex = idx
            }
        case .tts:
            SpeechManager.shared.speak(spot.tts.isEmpty ? spot.label : spot.tts)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            activeHotspotId = nil
        }
    }

    // MARK: - Background

    @ViewBuilder
    private func sceneBackgroundView(_ p: PageModel) -> some View {
        if let data = p.sceneImageData, let img = UIImage(data: data) {
            Image(uiImage: img)
                .resizable()
                .scaledToFill()
        } else {
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

extension UIImage {
    /// Downscales very large camera images so they persist and render efficiently on iPad.
    func downscaled(maxDimension: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxDimension, longest > 0 else { return self }
        let scale = maxDimension / longest
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            self.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
