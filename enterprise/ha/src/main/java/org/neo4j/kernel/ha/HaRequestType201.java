/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.ha;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;

import org.neo4j.com.ObjectSerializer;
import org.neo4j.com.RequestContext;
import org.neo4j.com.RequestType;
import org.neo4j.com.Response;
import org.neo4j.com.TargetCaller;
import org.neo4j.com.storecopy.ToNetworkStoreWriter;
import org.neo4j.helpers.ThisShouldNotHappenError;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.ha.com.master.HandshakeResult;
import org.neo4j.kernel.ha.com.master.Master;
import org.neo4j.kernel.ha.id.IdAllocation;
import org.neo4j.kernel.ha.lock.LockResult;
import org.neo4j.kernel.impl.nioneo.store.IdRange;
import org.neo4j.kernel.monitoring.Monitors;

import static org.neo4j.com.Protocol.INTEGER_SERIALIZER;
import static org.neo4j.com.Protocol.LONG_SERIALIZER;
import static org.neo4j.com.Protocol.VOID_SERIALIZER;
import static org.neo4j.com.Protocol.readBoolean;
import static org.neo4j.com.Protocol.readString;
import static org.neo4j.kernel.ha.com.slave.MasterClient.LOCK_SERIALIZER;

public enum HaRequestType201 implements RequestType<Master>
{
    // ====
    ALLOCATE_IDS( new TargetCaller<Master, IdAllocation>()
    {
        @Override
        public Response<IdAllocation> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            IdType idType = IdType.values()[input.readByte()];
            return master.allocateIds( context, idType );
        }
    }, new ObjectSerializer<IdAllocation>()
    {
        @Override
        public void write( IdAllocation idAllocation, ChannelBuffer result ) throws IOException
        {
            IdRange idRange = idAllocation.getIdRange();
            result.writeInt( idRange.getDefragIds().length );
            for ( long id : idRange.getDefragIds() )
            {
                result.writeLong( id );
            }
            result.writeLong( idRange.getRangeStart() );
            result.writeInt( idRange.getRangeLength() );
            result.writeLong( idAllocation.getHighestIdInUse() );
            result.writeLong( idAllocation.getDefragCount() );
        }
    }
    ),

    // ====
    CREATE_RELATIONSHIP_TYPE( new TargetCaller<Master, Integer>()
    {
        @Override
        public Response<Integer> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            return master.createRelationshipType( context, readString( input ) );
        }
    }, INTEGER_SERIALIZER ),

    // ====
    ACQUIRE_NODE_WRITE_LOCK( new AquireLockCall()
    {
        @Override
        public Response<LockResult> lock( Master master, RequestContext context, long... ids )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_NODE_READ_LOCK( new AquireLockCall()
    {
        @Override
        public Response<LockResult> lock( Master master, RequestContext context, long... ids )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_RELATIONSHIP_WRITE_LOCK( new AquireLockCall()
    {
        @Override
        public Response<LockResult> lock( Master master, RequestContext context, long... ids )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_RELATIONSHIP_READ_LOCK( new AquireLockCall()
    {
        @Override
        public Response<LockResult> lock( Master master, RequestContext context, long... ids )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    COMMIT( new TargetCaller<Master, Long>()
    {
        @Override
        public Response<Long> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
//            readString( input ); // Always neostorexadatasource
//            TransactionAccumulator accumulator = new TransactionAccumulator();
//            PhysicalTransactionCursor cursorReader = new PhysicalTransactionCursor(
//                    new NetworkReadableLogChannel( input ),
//                    new VersionAwareLogEntryReader( CommandReaderFactory.DEFAULT ),
//                    accumulator );
//
//            try
//            {
//                while( cursorReader.next() );
//            }
//            catch ( IOException e )
//            {
//                throw new RuntimeException( e );
//            }
//            finally
//            {
//                try
//                {
//                    cursorReader.close();
//                }
//                catch ( IOException e )
//                {
//                    throw new RuntimeException( e );
//                }
//            }
//            assert accumulator.getAccumulator().size() == 1 : "There should be only one transaction received when committing";
//            return master.commitSingleResourceTransaction( context, accumulator.getAccumulator().get( 0 ) );
            // TODO 2.2-future i have like, no idea how to handle this
            return null;
        }
    }, LONG_SERIALIZER ),

    // ====
    PULL_UPDATES( new TargetCaller<Master, Void>()
    {
        @Override
        public Response<Void> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            return master.pullUpdates( context );
        }
    }, VOID_SERIALIZER ),

    // ====
    FINISH( new TargetCaller<Master, Void>()
    {
        @Override
        public Response<Void> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            return master.finishTransaction( context, readBoolean( input ) );
        }
    }, VOID_SERIALIZER ),

    // ====
    HANDSHAKE( new TargetCaller<Master, HandshakeResult>()
    {
        @Override
        public Response<HandshakeResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            return master.handshake( input.readLong(), null );
        }
    }, new ObjectSerializer<HandshakeResult>()
    {
        @Override
        public void write( HandshakeResult responseObject, ChannelBuffer result ) throws IOException
        {
            result.writeInt( responseObject.txAuthor() );
            result.writeLong( responseObject.txChecksum() );
            result.writeLong( responseObject.epoch() );
        }
    }
    ),

    // ====
    COPY_STORE( new TargetCaller<Master, Void>()
    {
        @Override
        public Response<Void> call( Master master, RequestContext context, ChannelBuffer input,
                final ChannelBuffer target )
        {
            return master.copyStore( context, new ToNetworkStoreWriter( target, new Monitors() ) );
        }

    }, VOID_SERIALIZER ),

    // ====
    PLACEHOLDER_FOR_COPY_TRANSACTIONS( new TargetCaller<Master, Void>()
    {
        @Override
        public Response<Void> call( Master master, RequestContext context, ChannelBuffer input,
                final ChannelBuffer target )
        {
            throw new UnsupportedOperationException( "Not used anymore, merely here to keep the ordinal ids of the others" );
        }

    }, VOID_SERIALIZER ),

    // ====
    INITIALIZE_TX( new TargetCaller<Master, Void>()
    {
        @Override
        public Response<Void> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            return master.initializeTx( context );
        }
    }, VOID_SERIALIZER ),

    // ====
    ACQUIRE_GRAPH_WRITE_LOCK( new TargetCaller<Master, LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_GRAPH_READ_LOCK( new TargetCaller<Master, LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_INDEX_READ_LOCK( new TargetCaller<Master, LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }

    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_INDEX_WRITE_LOCK( new TargetCaller<Master, LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }

    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    PUSH_TRANSACTION( new TargetCaller<Master, Void>()
    {
        @Override
        public Response<Void> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
//            return master.pushTransaction( context, readString( input ), input.readLong() );
            throw new ThisShouldNotHappenError( "ChrisG", "Transaction pushing requests are obsolete" );
        }
    }, VOID_SERIALIZER ),

    // ====
    CREATE_PROPERTY_KEY( new TargetCaller<Master, Integer>()
    {
        @Override
        public Response<Integer> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            return master.createPropertyKey( context, readString( input ) );
        }
    }, INTEGER_SERIALIZER ),

    // ====
    CREATE_LABEL( new TargetCaller<Master, Integer>()
    {
        @Override
        public Response<Integer> call( Master master, RequestContext context, ChannelBuffer input,
                                       ChannelBuffer target )
        {
            return master.createLabel( context, readString( input ) );
        }
    }, INTEGER_SERIALIZER ),

    // ====
    ACQUIRE_SCHEMA_READ_LOCK( new TargetCaller<Master, LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }

    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },

    // ====
    ACQUIRE_SCHEMA_WRITE_LOCK( new TargetCaller<Master, LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }

    }, LOCK_SERIALIZER )
    {
        @Override
        public boolean isLock()
        {
            return true;
        }
    },
    ACQUIRE_INDEX_ENTRY_WRITE_LOCK( new TargetCaller<Master,LockResult>()
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context, ChannelBuffer input,
                                          ChannelBuffer target )
        {
            throw new ThisShouldNotHappenError( "Jake", "Older clients should not be allowed to talk to new masters" );
        }
    }, LOCK_SERIALIZER );


    @SuppressWarnings( "rawtypes" )
    final TargetCaller caller;
    @SuppressWarnings( "rawtypes" )
    final ObjectSerializer serializer;

    private <T> HaRequestType201( TargetCaller caller, ObjectSerializer<T> serializer )
    {
        this.caller = caller;
        this.serializer = serializer;
    }

    @Override
    public ObjectSerializer getObjectSerializer()
    {
        return serializer;
    }

    @Override
    public TargetCaller getTargetCaller()
    {
        return caller;
    }

    @Override
    public byte id()
    {
        return (byte) ordinal();
    }

    public boolean isLock()
    {
        return false;
    }

    private static abstract class AquireLockCall implements TargetCaller<Master, LockResult>
    {
        @Override
        public Response<LockResult> call( Master master, RequestContext context,
                                          ChannelBuffer input, ChannelBuffer target )
        {
            long[] ids = new long[input.readInt()];
            for ( int i = 0; i < ids.length; i++ )
            {
                ids[i] = input.readLong();
            }
            return lock( master, context, ids );
        }

        protected abstract Response<LockResult> lock( Master master, RequestContext context, long... ids );
    }
}