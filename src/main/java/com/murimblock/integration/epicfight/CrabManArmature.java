package com.murimblock.integration.epicfight;

import java.util.Map;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;

/** Joints of assets/murimblock/animmodels/entity/crab_man.json (21 joints, root "Root"). Names are case-sensitive. */
public final class CrabManArmature extends Armature {
    public final Joint torso;
    public final Joint head;
    public final Joint claw;
    public final Joint jaw;

    public CrabManArmature(String name, int jointNumber, Joint rootJoint, Map<String, Joint> jointMap) {
        super(name, jointNumber, rootJoint, jointMap);
        torso = getOrLogException(jointMap, "torso");
        head = getOrLogException(jointMap, "head");
        claw = getOrLogException(jointMap, "claw.R");
        jaw = getOrLogException(jointMap, "jaw_inner.R");
    }
}
