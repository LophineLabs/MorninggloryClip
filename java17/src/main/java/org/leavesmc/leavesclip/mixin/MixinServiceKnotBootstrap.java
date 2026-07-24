package org.leavesmc.leavesclip.mixin;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class MixinServiceKnotBootstrap implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "MorninggloryClip";
    }

    @Override
    public String getServiceClassName() {
        return "org.leavesmc.leavesclip.mixin.MixinServiceKnot";
    }

    @Override
    public void bootstrap() {
        // already done in Knot
    }
}
