/*
 * Copyright (c) 2018 Gomint team
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
@RegisterInfo(id = 513)
public class ItemShield extends ItemStack implements io.gomint.inventory.item.ItemShield {

    @Override
    public ItemType getType() {
        return ItemType.SHIELD;
    }

}
