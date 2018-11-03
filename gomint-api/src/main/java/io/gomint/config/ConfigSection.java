/*
 * Copyright (c) 2018 GoMint team
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author geNAZt
 * @version 1.0
 */
public class ConfigSection {

    public static ConfigSection convertFromMap( Map config ) {
        ConfigSection configSection = new ConfigSection();
        configSection.map.putAll( config );

        return configSection;

    }

    protected final Map<Object, Object> map = new LinkedHashMap<>();
    private String fullPath;

    public ConfigSection() {
        this.fullPath = "";
    }

    public ConfigSection( ConfigSection root, String key ) {
        this.fullPath = ( !root.fullPath.equals( "" ) ) ? root.fullPath + "." + key : key;
    }

    public ConfigSection create( String path ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "Cannot create section at empty path" );
        }

        //Be sure to have all ConfigSections down the Path
        int i1 = -1, i2;
        ConfigSection section = this;
        while ( ( i1 = path.indexOf( '.', i2 = i1 + 1 ) ) != -1 ) {
            String node = path.substring( i2, i1 );
            ConfigSection subSection = section.getConfigSection( node );

            //This subsection does not exists create one
            if ( subSection == null ) {
                section = section.create( node );
            } else {
                section = subSection;
            }
        }

        String key = path.substring( i2 );
        if ( section == this ) {
            ConfigSection result = new ConfigSection( this, key );
            map.put( key, result );
            return result;
        }

        return section.create( key );
    }

    public void set( String path, Object value ) {
        set( path, value, true );
    }

    public void set( String path, Object value, boolean searchForSubNodes ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "Cannot set a value at empty path" );
        }

        //Be sure to have all ConfigSections down the Path
        int i1 = -1, i2 = 0;
        ConfigSection section = this;

        if ( searchForSubNodes ) {
            while ( ( i1 = path.indexOf( '.', i2 = i1 + 1 ) ) != -1 ) {
                String node = path.substring( i2, i1 );
                ConfigSection subSection = section.getConfigSection( node );

                if ( subSection == null ) {
                    section = section.create( node );
                } else {
                    section = subSection;
                }
            }
        }

        String key = path.substring( i2 );
        if ( section == this ) {
            if ( value == null ) {
                map.remove( key );
            } else {
                map.put( key, value );
            }
        } else {
            section.set( key, value );
        }
    }

    public Map<Object, Object> getValues( boolean deep ) {
        Map<Object, Object> result = new LinkedHashMap<>();
        mapChildrenValues( result, this, deep );
        return result;
    }

    public void remove( String path ) {
        this.set( path, null );
    }

    public boolean has( String path ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "Cannot remove a Value at empty path" );
        }

        //Be sure to have all ConfigSections down the Path
        int i1 = -1, i2;
        ConfigSection section = this;
        while ( ( i1 = path.indexOf( '.', i2 = i1 + 1 ) ) != -1 ) {
            String node = path.substring( i2, i1 );
            ConfigSection subSection = section.getConfigSection( node );

            if ( subSection == null ) {
                return false;
            } else {
                section = subSection;
            }
        }

        String key = path.substring( i2 );
        if ( section == this ) {
            return map.containsKey( key );
        } else {
            return section.has( key );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T get( String path ) {
        if ( path == null ) {
            throw new IllegalArgumentException( "Cannot remove a Value at empty path" );
        }

        //Be sure to have all ConfigSections down the Path
        int i1 = -1, i2;
        ConfigSection section = this;
        while ( ( i1 = path.indexOf( '.', i2 = i1 + 1 ) ) != -1 ) {
            String node = path.substring( i2, i1 );
            ConfigSection subSection = section.getConfigSection( node );

            if ( subSection == null ) {
                section = section.create( node );
            } else {
                section = subSection;
            }
        }

        String key = path.substring( i2 );
        if ( section == this ) {
            return (T) map.get( key );
        } else {
            return section.get( key );
        }
    }

    public Map getRawMap() {
        return map;
    }

    protected void mapChildrenValues( Map<Object, Object> output, ConfigSection section, boolean deep ) {
        if ( section != null ) {
            for ( Map.Entry<Object, Object> entry : section.map.entrySet() ) {
                if ( entry.getValue() instanceof ConfigSection ) {
                    Map<Object, Object> result = new LinkedHashMap<>();

                    output.put( entry.getKey(), result );

                    if ( deep ) {
                        mapChildrenValues( result, (ConfigSection) entry.getValue(), true );
                    }
                } else {
                    output.put( entry.getKey(), entry.getValue() );
                }
            }
        }
    }

    private ConfigSection getConfigSection( String node ) {
        return ( map.containsKey( node ) && map.get( node ) instanceof ConfigSection ) ? (ConfigSection) map.get( node ) : null;
    }

}
