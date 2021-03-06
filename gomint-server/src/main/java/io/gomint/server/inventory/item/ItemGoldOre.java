package io.gomint.server.inventory.item;
import io.gomint.inventory.item.ItemType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 14 )
 public class ItemGoldOre extends ItemStack implements io.gomint.inventory.item.ItemGoldOre {



    @Override
    public String getBlockId() {
        return "minecraft:gold_ore";
    }

    @Override
    public ItemType getType() {
        return ItemType.GOLD_ORE;
    }

}
