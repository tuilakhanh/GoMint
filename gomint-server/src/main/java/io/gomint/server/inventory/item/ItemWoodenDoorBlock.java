package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 64 )
public class ItemWoodenDoorBlock extends ItemStack {



    @Override
    public String getBlockId() {
        return "minecraft:wooden_door";
    }

    @Override
    public ItemType getType() {
        return ItemType.WOODEN_DOOR_BLOCK;
    }

}
