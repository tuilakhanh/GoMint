/*
 * Copyright (c) 2018 GoMint team
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.config.converter;

import io.gomint.config.BaseConfigMapper;
import io.gomint.config.InternalConverter;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author geNAZt
 * @version 1.0
 */
public class ListConverter implements Converter {

    private InternalConverter internalConverter;

    public ListConverter( InternalConverter internalConverter ) {
        this.internalConverter = internalConverter;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Object toConfig( Class<?> type, Object obj, ParameterizedType genericType ) throws Exception {
        List values = (List) obj;
        List newList = new ArrayList();

        if ( this.internalConverter.getConfig() instanceof BaseConfigMapper ) {
            BaseConfigMapper baseConfigMapper = (BaseConfigMapper) this.internalConverter.getConfig();
            baseConfigMapper.addCommentPrefix( "-" );
        }

        for ( Object val : values ) {
            Converter converter = this.internalConverter.getConverter( val.getClass() );

            if ( converter != null ) {
                newList.add( converter.toConfig( val.getClass(), val, null ) );
            } else {
                newList.add( val );
            }
        }

        if ( this.internalConverter.getConfig() instanceof BaseConfigMapper ) {
            BaseConfigMapper baseConfigMapper = (BaseConfigMapper) this.internalConverter.getConfig();
            baseConfigMapper.removeCommentPrefix( "-" );
        }

        return newList;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Object fromConfig( Class type, Object section, ParameterizedType genericType ) throws Exception {
        List newList = new ArrayList();
        try {
            newList = ( (List) type.newInstance() );
        } catch ( Exception e ) {
        }

        List values = (List) section;

        if ( genericType != null && genericType.getActualTypeArguments()[0] instanceof Class ) {
            Converter converter = this.internalConverter.getConverter( (Class) genericType.getActualTypeArguments()[0] );

            if ( converter != null ) {
                for ( int i = 0; i < values.size(); i++ ) {
                    newList.add( converter.fromConfig( (Class) genericType.getActualTypeArguments()[0], values.get( i ), null ) );
                }
            } else {
                newList = values;
            }
        } else {
            newList = values;
        }

        return newList;
    }

    @Override
    public boolean supports( Class<?> type ) {
        return List.class.isAssignableFrom( type );
    }

}
