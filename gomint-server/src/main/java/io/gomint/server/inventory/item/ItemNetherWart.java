/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemType;
import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 372 )
public class ItemNetherWart extends ItemStack implements io.gomint.inventory.item.ItemNetherWart {

    @Override
    public ItemType getType() {
        return ItemType.NETHER_WART;
    }

    @Override
    public String getBlockId() {
        return "minecraft:nether_wart";
    }

}
