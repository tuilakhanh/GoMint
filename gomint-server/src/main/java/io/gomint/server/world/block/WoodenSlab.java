package io.gomint.server.world.block;

import io.gomint.inventory.item.ItemStack;
import io.gomint.server.world.block.helper.ToolPresets;
import io.gomint.server.world.block.state.EnumBlockState;
import io.gomint.world.block.BlockType;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.world.block.BlockWoodenSlab;
import io.gomint.world.block.data.WoodType;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:wooden_slab" )
public class WoodenSlab extends Slab implements BlockWoodenSlab {

    private EnumBlockState<WoodType> variant = new EnumBlockState<>( this, WoodType.values() );

    @Override
    public String getBlockId() {
        return "minecraft:wooden_slab";
    }

    @Override
    public long getBreakTime() {
        return 3000;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public float getBlastResistance() {
        return 15.0f;
    }

    @Override
    public BlockType getType() {
        return BlockType.WOODEN_SLAB;
    }

    @Override
    public boolean canBeBrokenWithHand() {
        return true;
    }

    @Override
    public Class<? extends ItemStack>[] getToolInterfaces() {
        return ToolPresets.AXE;
    }

    @Override
    public WoodType getWoodType() {
        return this.variant.getState();
    }

    @Override
    public void setWoodType( WoodType woodType ) {
        this.variant.setState( woodType );
    }

}
