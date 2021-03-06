package org.infinispan.persistence.dummy;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.elements.ElementDefinition;
import org.infinispan.configuration.cache.AbstractSegmentedStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;

@BuiltBy(DummyInMemoryStoreConfigurationBuilder.class)
@ConfigurationFor(DummyInMemoryStore.class)
public class DummyInMemoryStoreConfiguration extends AbstractSegmentedStoreConfiguration<DummyInMemoryStoreConfiguration> implements ConfigurationInfo {
   static final AttributeDefinition<Boolean> SLOW = AttributeDefinition.builder("slow", false).immutable().build();
   static final AttributeDefinition<String> STORE_NAME = AttributeDefinition.builder("storeName", null, String.class).immutable().build();
   static final AttributeDefinition<Integer> START_FAILURES = AttributeDefinition.builder("startFailures", 0).immutable().build();
   public static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(DummyInMemoryStoreConfiguration.class, AbstractStoreConfiguration.attributeDefinitionSet(), SLOW, STORE_NAME, START_FAILURES);
   }

   static ElementDefinition ELEMENT_DEFINITION = new ElementDefinition() {
      @Override
      public boolean isTopLevel() {
         return true;
      }

      @Override
      public ElementOutput toExternalName(ConfigurationInfo configuration) {
         return new ElementOutput("store", DummyInMemoryStore.class.getName());
      }

      @Override
      public boolean isSynthetic(String attribute) {
         return attribute.equals("class");
      }

      @Override
      public boolean supports(String name) {
         return false;
      }
   };

   public DummyInMemoryStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore) {
      super(attributes, async, singletonStore);
   }

   @Override
   public ElementDefinition getElementDefinition() {
      return ELEMENT_DEFINITION;
   }

   @Override
   public DummyInMemoryStoreConfiguration newConfigurationFrom(int segment) {
      AttributeSet set = DummyInMemoryStoreConfiguration.attributeDefinitionSet();
      set.read(attributes);
      String storeName = set.attribute(STORE_NAME).get();
      if (storeName != null) {
         set.attribute(STORE_NAME).set(storeName + "-" + segment);
      }
      return new DummyInMemoryStoreConfiguration(set.protect(), async(), singletonStore());
   }

   public boolean slow() {
      return attributes.attribute(SLOW).get();
   }

   public String storeName() {
      return attributes.attribute(STORE_NAME).get();
   }

   public int startFailures() {
      return attributes.attribute(START_FAILURES).get();
   }
}
