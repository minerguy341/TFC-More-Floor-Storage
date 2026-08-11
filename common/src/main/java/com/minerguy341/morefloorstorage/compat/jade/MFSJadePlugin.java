package com.minerguy341.morefloorstorage.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import com.minerguy341.morefloorstorage.common.block.PileBlock;

/**
 * Entry point Jade finds by annotation scan, which is the only thing that loads any class in this
 * package. Nothing in the mod proper refers to it, so with Jade absent none of these classes are ever
 * resolved and its types never have to exist.
 * <p>
 * Compiling this package is conditional too - see {@code jadeVersion} in the convention plugin.
 */
@WailaPlugin
public class MFSJadePlugin implements IWailaPlugin
{
    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        // Piles keep their contents in the block entity and sync the lot to the client already, since
        // that is what the renderer draws from. So there is nothing for a server data provider to send.
        registration.registerBlockComponent(PileContentsProvider.INSTANCE, PileBlock.class);
    }
}
