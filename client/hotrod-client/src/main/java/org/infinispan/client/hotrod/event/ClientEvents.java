package org.infinispan.client.hotrod.event;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.filter.Filters;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.remote.client.ContinuousQueryResult;

import java.io.IOException;

import static org.infinispan.client.hotrod.filter.Filters.*;

public class ClientEvents {

   /**
    * The name of the factory used for query DSL based filters and converters. This factory is provided internally by
    * the server.
    * @deprecated Use constants from {@link Filters}
    */
   @Deprecated
   public static final String QUERY_DSL_FILTER_FACTORY_NAME = Filters.QUERY_DSL_FILTER_FACTORY_NAME;

   @Deprecated
   public static final String CONTINUOUS_QUERY_FILTER_FACTORY_NAME = Filters.CONTINUOUS_QUERY_FILTER_FACTORY_NAME;

   private static final ClientCacheFailoverEvent FAILOVER_EVENT_SINGLETON = new ClientCacheFailoverEvent() {
      @Override
      public ClientEvent.Type getType() {
         return ClientEvent.Type.CLIENT_CACHE_FAILOVER;
      }
   };

   private ClientEvents() {
      // Static helper class, cannot be constructed
   }

   public static ClientCacheFailoverEvent mkCachefailoverEvent() {
      return FAILOVER_EVENT_SINGLETON;
   }

   /**
    * Register a client listener that uses a query DSL based filter. The listener is expected to be annotated such that
    * {@link org.infinispan.client.hotrod.annotation.ClientListener#useRawData} = true and {@link
    * org.infinispan.client.hotrod.annotation.ClientListener#filterFactoryName} and {@link
    * org.infinispan.client.hotrod.annotation.ClientListener#converterFactoryName} are equal to {@link
    * Filters#QUERY_DSL_FILTER_FACTORY_NAME}
    *
    * @param remoteCache the remote cache to attach the listener
    * @param listener    the listener instance
    * @param query       the query to be used for filtering and conversion (if projections are used)
    */
   public static void addClientQueryListener(RemoteCache<?, ?> remoteCache, Object listener, Query query) {
      ClientListener l = listener.getClass().getAnnotation(ClientListener.class);
      if (!l.useRawData()) {
         throw new IllegalArgumentException("The client listener must use raw data");
      }
      if (!l.filterFactoryName().equals(Filters.QUERY_DSL_FILTER_FACTORY_NAME)) {
         throw new IllegalArgumentException("The client listener must use the '" + Filters.QUERY_DSL_FILTER_FACTORY_NAME + "' filter factory");
      }
      if (!l.converterFactoryName().equals(Filters.QUERY_DSL_FILTER_FACTORY_NAME)) {
         throw new IllegalArgumentException("The client listener must use the '" + Filters.QUERY_DSL_FILTER_FACTORY_NAME + "' converter factory");
      }
      Object[] factoryParams = makeFactoryParams(query);
      remoteCache.addClientListener(listener, factoryParams, null);
   }

   /**
    * Register a continuous query listener that uses a query DSL based filter. The listener will receive notifications
    * when a cache entry joins or leaves the matching set.
    *
    * @param remoteCache   the remote cache to attach the listener
    * @param queryListener the listener instance
    * @param query         the query to be used for determining the matching set
    */
   public static Object addContinuousQueryListener(RemoteCache<?, ?> remoteCache, ContinuousQueryListener queryListener, Query query) {
      SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(remoteCache.getRemoteCacheManager());
      ClientEntryListener eventListener = new ClientEntryListener(serCtx, queryListener);
      Object[] factoryParams = makeFactoryParams(query);
      remoteCache.addClientListener(eventListener, factoryParams, null);
      return eventListener;
   }

   @ClientListener(filterFactoryName = Filters.CONTINUOUS_QUERY_FILTER_FACTORY_NAME,
         converterFactoryName = Filters.CONTINUOUS_QUERY_FILTER_FACTORY_NAME,
         useRawData = true, includeCurrentState = true)
   private static final class ClientEntryListener {

      private final SerializationContext serializationContext;

      private final ContinuousQueryListener queryListener;

      public ClientEntryListener(SerializationContext serializationContext, ContinuousQueryListener queryListener) {
         this.serializationContext = serializationContext;
         this.queryListener = queryListener;
      }

      @ClientCacheEntryCreated
      @ClientCacheEntryModified
      @ClientCacheEntryRemoved
      @ClientCacheEntryExpired
      public void handleClientCacheEntryCreatedEvent(ClientCacheEntryCustomEvent event) throws IOException {
         ContinuousQueryResult cqresult = ProtobufUtil.fromByteArray(serializationContext, (byte[]) event.getEventData(), ContinuousQueryResult.class);
         Object key = ProtobufUtil.fromWrappedByteArray(serializationContext, cqresult.getKey());
         Object value = cqresult.getValue() != null ? ProtobufUtil.fromWrappedByteArray(serializationContext, cqresult.getValue()) : null;
         if (cqresult.isJoining()) {
            queryListener.resultJoining(key, value);
         } else {
            queryListener.resultLeaving(key);
         }
      }
   }
}
