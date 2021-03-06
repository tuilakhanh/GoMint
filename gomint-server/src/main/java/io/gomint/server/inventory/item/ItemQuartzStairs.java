package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 156 )
 public class ItemQuartzStairs extends ItemStack implements io.gomint.inventory.item.ItemQuartzStairs {



    @Override
    public String getBlockId() {
        return "minecraft:quartz_stairs";
    }

    @Override
    public ItemType getType() {
        return ItemType.QUARTZ_STAIRS;
    }

}
