package io.gomint.entity.passive;

import io.gomint.GoMint;
import io.gomint.entity.EntityAgeable;

/**
 * @author KingAli
 * @version 1.0
 */
public interface EntityFox extends EntityAgeable {

    /**
     * Create a new entity horse with no config
     *
     * @return empty, fresh horse
     */
    static EntityFox create() {
        return GoMint.instance().createEntity( EntityFox.class );
    }
}
