package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 247 )
 public class ItemNetherReactorCore extends ItemStack implements io.gomint.inventory.item.ItemNetherReactorCore {



    @Override
    public String getBlockId() {
        return "minecraft:netherreactor";
    }

    @Override
    public ItemType getType() {
        return ItemType.NETHER_REACTOR_CORE;
    }

}
