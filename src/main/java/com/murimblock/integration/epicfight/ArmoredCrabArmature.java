package com.murimblock.integration.epicfight;

import java.util.Map;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;

/**
 * Joints of assets/murimblock/animmodels/entity/armored_crab.json (28 joints, root "Root").
 * Only the joints Murimblock code refers to are named here; names are case-sensitive.
 */
public final class ArmoredCrabArmature extends Armature {
    public final Joint body;
    public final Joint clawRightPalm;
    public final Joint clawLeftPalm;

    public ArmoredCrabArmature(String name, int jointNumber, Joint rootJoint, Map<String, Joint> jointMap) {
        super(name, jointNumber, rootJoint, jointMap);
        body = getOrLogException(jointMap, "body");
        clawRightPalm = getOrLogException(jointMap, "claw_r_palm");
        clawLeftPalm = getOrLogException(jointMap, "claw_l_palm");
    }
}
