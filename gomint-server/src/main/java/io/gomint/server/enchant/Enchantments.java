/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.enchant;

import io.gomint.server.registry.Generator;
import io.gomint.server.registry.Registry;
import io.gomint.server.util.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Enchantments {

    private static final Logger LOGGER = LoggerFactory.getLogger( Enchantments.class );
    private final Registry<Enchantment> generators;

    public Enchantments( ClassPath classPath ) {
        this.generators = new Registry<>( classPath, clazz -> () -> {
            try {
                return (Enchantment) clazz.newInstance();
            } catch ( InstantiationException | IllegalAccessException e ) {
                LOGGER.error( "Could not generate new enchantment", e );
            }

            return null;
        } );

        this.generators.register( "io.gomint.server.enchant" );
        this.generators.cleanup();
    }

    /**
     * Create enchantment
     *
     * @param id  of the enchantment
     * @param lvl of the enchantment
     * @return new enchantment instance which contains level data
     */
    public Enchantment create( short id, short lvl ) {
        Generator<Enchantment> enchantmentGenerator = this.generators.getGenerator( id );
        if ( enchantmentGenerator == null ) {
            LOGGER.warn( "Unknown enchant {}", id );
            return null;
        }

        Enchantment enchantment = enchantmentGenerator.generate();
        enchantment.setLevel( lvl );
        return enchantment;
    }

    public short getId( Class<? extends io.gomint.enchant.Enchantment> clazz ) {
        return (short) this.generators.getId( clazz );
    }

}
