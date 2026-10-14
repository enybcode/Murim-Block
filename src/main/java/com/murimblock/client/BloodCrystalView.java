package com.murimblock.client;

/** Which Sanguine Art crystals a client leaves out of its own view. */
final class BloodCrystalView {
    private BloodCrystalView() {
    }

    /**
     * A back crystal of this client's own player, seen through their eyes: the crystals float round the head, so in
     * first person they would fill the screen. Other players, and the third-person view, still show them.
     */
    static boolean hiddenInFirstPerson(boolean dorsal, boolean ownedByViewer, boolean viewerIsCamera, boolean firstPerson) {
        return dorsal && ownedByViewer && viewerIsCamera && firstPerson;
    }
}
