package com.talktiles.tablet

/**
 * The built-in example scenes: an illustrated room with talking spots on
 * its objects, so a new family sees what a visual scene does before making
 * one from their own photo. Pictures are assets `Scenes/<key>.webp`
 * (drawn with the same model as the symbol set); spots are percent of the
 * picture. Tapping one speaks it in Bella's voice; the kitchen fridge
 * shows the other kind of spot, one that opens a page.
 */
object ExampleScenes {
    data class Spot(val label: String, val says: String, val x: Double, val y: Double, val w: Double, val h: Double, val opensPage: String? = null)
    data class Scene(val key: String, val title: String, val summary: String, val spots: List<Spot>)

    /** The two that go in the starter book. */
    val starterKeys = listOf("kitchen", "playground")

    val all: List<Scene> = listOf(
        Scene("kitchen", "Kitchen", "Fridge, sink, stove, apples, milk", listOf(
            Spot("Fridge", "Let's look for food", 0.0, 8.0, 24.0, 86.0, opensPage = "Food & Snacks"),
            Spot("Window", "Look outside", 36.0, 8.0, 22.0, 32.0),
            Spot("Cookies", "I want a cookie", 59.0, 38.0, 8.0, 11.0),
            Spot("Sink", "Wash my hands", 35.0, 47.0, 23.0, 10.0),
            Spot("Stove", "The stove is hot", 74.0, 41.0, 26.0, 17.0),
            Spot("Apples", "I want an apple", 37.0, 56.0, 16.0, 18.0),
            Spot("Milk", "I want milk", 54.0, 60.0, 7.0, 13.0)
        )),
        Scene("living_room", "Living Room", "Couch, TV, lamp, books, toys", listOf(
            Spot("Books", "Let's read a book", 0.0, 5.0, 14.0, 75.0),
            Spot("Picture", "Look at the picture", 34.0, 16.0, 22.0, 20.0),
            Spot("Lamp", "Turn on the light", 14.0, 26.0, 9.0, 43.0),
            Spot("Couch", "Let's sit on the couch", 19.0, 43.0, 54.0, 29.0),
            Spot("Plant", "Look at the plant", 67.0, 26.0, 15.0, 40.0),
            Spot("TV", "I want to watch TV", 82.0, 35.0, 15.0, 28.0),
            Spot("Toys", "I want to play with my toys", 82.0, 70.0, 18.0, 30.0)
        )),
        Scene("playground", "Playground", "Slide, swings, seesaw, sandbox, tree", listOf(
            Spot("Climb", "I want to climb", 0.0, 4.0, 21.0, 33.0),
            Spot("Slide", "I want to go down the slide", 8.0, 38.0, 36.0, 32.0),
            Spot("Swings", "Push me on the swing", 45.0, 27.0, 35.0, 39.0),
            Spot("Tree", "Look at the big tree", 80.0, 0.0, 20.0, 62.0),
            Spot("Seesaw", "Let's go on the seesaw", 4.0, 70.0, 40.0, 23.0),
            Spot("Sandbox", "Let's play in the sand", 54.0, 68.0, 46.0, 27.0)
        )),
        Scene("bedroom", "Bedroom", "Bed, pajamas, moon, teddy bear, lamp", listOf(
            Spot("Moon", "Good night moon", 9.0, 2.0, 32.0, 48.0),
            Spot("Picture", "Look at the picture", 57.0, 10.0, 17.0, 22.0),
            Spot("Teddy bear", "I want my teddy bear", 81.0, 10.0, 11.0, 13.0),
            Spot("Bed", "I am tired", 10.0, 36.0, 68.0, 56.0),
            Spot("Pajamas", "Time for pajamas", 22.0, 58.0, 23.0, 20.0),
            Spot("Lamp", "Turn off the light", 82.0, 42.0, 10.0, 18.0),
            Spot("Slippers", "Where are my slippers", 60.0, 85.0, 17.0, 11.0)
        )),
        Scene("bathroom", "Bathroom", "Toilet, bath, duck, sink, toothbrush", listOf(
            Spot("Mirror", "Look in the mirror", 78.0, 8.0, 22.0, 42.0),
            Spot("Towel", "I need a towel", 65.0, 38.0, 8.0, 18.0),
            Spot("Bath", "Time for a bath", 0.0, 60.0, 37.0, 40.0),
            Spot("Duck", "My rubber duck", 8.0, 65.0, 13.0, 20.0),
            Spot("Toilet", "I need to go to the bathroom", 42.0, 53.0, 14.0, 40.0),
            Spot("Wash hands", "Wash my hands", 61.0, 55.0, 28.0, 17.0),
            Spot("Brush teeth", "Brush my teeth", 89.0, 51.0, 9.0, 22.0),
            Spot("Soap", "Use the soap", 84.0, 74.0, 14.0, 9.0)
        ))
    )

    fun byKey(key: String): Scene? = all.firstOrNull { it.key == key }

    /**
     * A page for one scene. `pageIds` maps page titles to ids so a spot that
     * opens a page can point at it; a spot whose page is missing just speaks.
     */
    fun makePage(scene: Scene, pageIds: Map<String, String> = emptyMap()): PageModel {
        val spots = scene.spots.mapIndexed { i, s ->
            val target = s.opensPage?.let { pageIds[it] }
            HotspotModel(id = i + 1, x = s.x, y = s.y, w = s.w, h = s.h, label = s.label, tts = s.says,
                style = HotspotStyle.HIGHLIGHT,
                action = if (target != null) HotspotAction.JUMP else HotspotAction.TTS, jumpPageId = target)
        }
        return PageModel(title = scene.title, type = PageType.SCENE, gridSize = 4, hotspots = spots, scenePresetKey = scene.key)
    }
}
