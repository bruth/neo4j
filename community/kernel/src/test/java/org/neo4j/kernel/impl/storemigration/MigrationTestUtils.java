/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.storemigration;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.helpers.UTF8;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.kernel.DefaultFileSystemAbstraction;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.storemigration.legacystore.v19.Legacy19Store;
import org.neo4j.kernel.impl.storemigration.legacystore.v20.Legacy20Store;
import org.neo4j.test.Unzip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.neo4j.kernel.impl.util.IoPrimitiveUtils.readAndFlip;

public class MigrationTestUtils
{
    public static Config defaultConfig()
    {
        return defaultConfig( MapUtil.stringMap() );
    }

    public static Config defaultConfig( Map<String, String> inputParams )
    {
        return new Config( inputParams, GraphDatabaseSettings.class );
    }

    public static int[] makeLongArray()
    {
        int[] longArray = new int[100];
        for ( int i = 0; i < 100; i++ )
        {
            longArray[i] = i;
        }
        return longArray;
    }

    public static String makeLongString()
    {
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < 100; i++ )
        {
            builder.append( "characters" );
        }
        return builder.toString();
    }

    public static void changeVersionNumber( FileSystemAbstraction fileSystem, File storeFile, String versionString )
            throws IOException
    {
        byte[] versionBytes = UTF8.encode( versionString );
        StoreChannel fileChannel = fileSystem.open( storeFile, "rw" );
        fileChannel.position( fileSystem.getFileSize( storeFile ) - versionBytes.length );
        fileChannel.write( ByteBuffer.wrap( versionBytes ) );
        fileChannel.close();
    }

    public static void truncateFile( FileSystemAbstraction fileSystem, File storeFile,
            String suffixToDetermineTruncationLength ) throws IOException
    {
        byte[] versionBytes = UTF8.encode( suffixToDetermineTruncationLength );
        StoreChannel fileChannel = fileSystem.open( storeFile, "rw" );
        fileChannel.truncate( fileSystem.getFileSize( storeFile ) - versionBytes.length );
        fileChannel.close();
    }

    public static void truncateAllFiles( FileSystemAbstraction fileSystem, File workingDirectory ) throws IOException
    {
        for ( StoreFile20 storeFile : StoreFile20.legacyStoreFiles() )
        {
            truncateFile( fileSystem, new File( workingDirectory, storeFile.storeFileName() ),
                    storeFile.legacyVersion() );
        }
    }

    public static void truncateToFixedLength( FileSystemAbstraction fileSystem, File storeFile, int newLength )
            throws IOException
    {
        StoreChannel fileChannel = fileSystem.open( storeFile, "rw" );
        fileChannel.truncate( newLength );
        fileChannel.close();
    }

    public static void prepareSampleLegacyDatabase( EphemeralFileSystemAbstraction workingFs,
            File workingDirectory ) throws IOException
    {
        File resourceDirectory = find20FormatStoreDirectory();
        workingFs.copyRecursivelyFromOtherFs( resourceDirectory, new DefaultFileSystemAbstraction(), workingDirectory );
    }

    public static void prepareSampleLegacyDatabase( FileSystemAbstraction workingFs, File workingDirectory ) throws IOException
    {
        File resourceDirectory = find20FormatStoreDirectory();

        workingFs.deleteRecursively( workingDirectory );
        workingFs.mkdirs( workingDirectory );

        // TODO only works with DefaultFileSystemAbstraction
        FileUtils.copyRecursively( resourceDirectory, workingDirectory );
    }

    public static File find20FormatStoreDirectory() throws IOException
    {
        return Unzip.unzip( Legacy20Store.class, "exampledb.zip" );
    }

    public static File find19FormatStoreDirectory( File unzipTarget ) throws IOException
    {
        return Unzip.unzip( Legacy19Store.class, "upgradeTest19Db.zip", unzipTarget );
    }

    public static File find20FormatStoreDirectory( File unzipTarget ) throws IOException
    {
        return Unzip.unzip( Legacy20Store.class, "exampledb.zip", unzipTarget );
    }

    public static boolean allStoreFilesHaveVersion( FileSystemAbstraction fileSystem, File workingDirectory,
            String version ) throws IOException
    {
        for ( StoreFile20 storeFile : StoreFile20.legacyStoreFiles() )
        {
            StoreChannel channel = fileSystem.open( new File( workingDirectory, storeFile.storeFileName() ), "r" );
            int length = UTF8.encode( version ).length;
            byte[] bytes = new byte[length];
            ByteBuffer buffer = ByteBuffer.wrap( bytes );
            channel.position( channel.size() - length );
            channel.read( buffer );
            channel.close();

            String foundVersion = UTF8.decode( bytes );
            if ( !version.equals( foundVersion ) )
            {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAnyStoreFiles( FileSystemAbstraction fileSystem, File directory )
    {
        for ( StoreFile20 file : StoreFile20.values() )
        {
            if ( fileSystem.fileExists( new File( directory, file.storeFileName() ) ) )
            {
                return true;
            }
        }
        return false;
    }

    public static void verifyFilesHaveSameContent( FileSystemAbstraction fileSystem, File original,
            File other ) throws IOException
    {
        for ( File originalFile : fileSystem.listFiles( original ) )
        {
            File otherFile = new File( other, originalFile.getName() );
            if ( !fileSystem.isDirectory( originalFile ) )
            {
                StoreChannel originalChannel = fileSystem.open( originalFile, "r" );
                StoreChannel otherChannel = fileSystem.open( otherFile, "r" );
                try
                {
                    ByteBuffer buffer = ByteBuffer.allocate( 1 );
                    while( true )
                    {
                        if ( !readAndFlip( originalChannel, buffer, 1 ) )
                        {
                            break;
                        }
                        int originalByte = buffer.get();

                        if ( !readAndFlip( otherChannel, buffer, 1 ) )
                        {
                            fail( "Files have different sizes" );
                        }
                        assertEquals( "Different content in " + originalFile.getName(), originalByte, buffer.get() );
                    }
                }
                finally
                {
                    originalChannel.close();
                    otherChannel.close();
                }
            }
        }
    }

    public static File isolatedMigrationDirectoryOf( File dbDirectory )
    {
        return new File( dbDirectory, "upgrade" );
    }
}
