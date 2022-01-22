/*
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.kafka;

import nl.clockwork.ebms.delivery.task.DeliveryTask;
import nl.clockwork.ebms.delivery.task.DeliveryTaskHandlerConfig;
import nl.clockwork.ebms.model.EbMSMessageProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import lombok.val;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@Conditional(DeliveryTaskHandlerConfig.KafkaTaskHandlerType.class)
@EnableKafka
public class KafkaConfig
{
	private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

	@Value("${kafka.serverUrl}")
	private String kafkaServerUrl;
	private String OFFSET = "earliest";

	@PostConstruct
	public void init()
	{
		logger.info("init<KafkaConfig>");
	}

	@Bean
	public ProducerFactory<String,EbMSMessageProperties> messagePropertiesProducerFactory()
	{
		return new DefaultKafkaProducerFactory<>(messageProducerConfiguration());
	}

	@Bean
	public ProducerFactory<String,DeliveryTask> deliveryTaskProducerFactory()
	{
		return new DefaultKafkaProducerFactory<>(deliveryTaskProducerConfiguration());
	}

	@Bean(name = "deliveryTaskKafkaTemplate")
	public KafkaTemplate<String,DeliveryTask> deliveryTaskKafkaTemplate()
	{
		logger.info("Initializing deliveryTaskKafkaTemplate");
		return new KafkaTemplate<>(deliveryTaskProducerFactory());
	}

	@Bean(name = "messagePropertiesKafkaTemplate")
	public KafkaTemplate<String,EbMSMessageProperties> messagePropertiesKafkaTemplate()
	{
		logger.info("Initializing messagePropertiesKafkaTemplate");
		return new KafkaTemplate<>(messagePropertiesProducerFactory());
	}

	@Bean
	public Map<String,Object> deliveryTaskProducerConfiguration()
	{
		val configurations = new HashMap<String,Object>();
		configurations.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaServerUrl);
		configurations.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
		configurations.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,JsonSerializer.class);
		configurations.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,true);
		configurations.put(ProducerConfig.CLIENT_ID_CONFIG,UUID.randomUUID().toString());
		configurations.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,"delivery-task-transaction");
		return configurations;
	}

	@Bean
	public Map<String,Object> messageProducerConfiguration()
	{
		val configurations = new HashMap<String,Object>();
		configurations.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaServerUrl);
		configurations.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
		configurations.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,JsonSerializer.class);
		configurations.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,true);
		configurations.put(ProducerConfig.CLIENT_ID_CONFIG,UUID.randomUUID().toString());
		configurations.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,"message-property-transaction");
		return configurations;
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String,DeliveryTask> kafkaListenerContainerFactory()
	{
		val factory = new ConcurrentKafkaListenerContainerFactory<String,DeliveryTask>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}

	@Bean
	public DefaultKafkaConsumerFactory<String,DeliveryTask> consumerFactory()
	{
		val deliveryTaskDeserializer = new JsonDeserializer<>(DeliveryTask.class);
		deliveryTaskDeserializer.addTrustedPackages("nl.clockwork.ebms.delivery.task.DeliveryTask");
		return new DefaultKafkaConsumerFactory<String,DeliveryTask>(consumerConfigurations(),new StringDeserializer(),deliveryTaskDeserializer);
	}

	@Bean
	public Map<String,Object> consumerConfigurations()
	{
		val configurations = new HashMap<String,Object>();
		configurations.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaServerUrl);
		configurations.put(ConsumerConfig.CLIENT_ID_CONFIG,UUID.randomUUID().toString());
		configurations.put(ConsumerConfig.GROUP_ID_CONFIG,"EBMS");
		configurations.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
		configurations.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,JsonDeserializer.class);
		configurations.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,OFFSET);
		configurations.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);
		configurations.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,"read_committed");
		return configurations;
	}

	@Bean
	public KafkaTransactionManager<String,DeliveryTask> deliveryTaskKafkaTransactionManager()
	{
		KafkaTransactionManager<String,DeliveryTask> ktm = new KafkaTransactionManager<String,DeliveryTask>(deliveryTaskProducerFactory());
		ktm.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		return ktm;
	}

	@Bean
	public KafkaTransactionManager<String,EbMSMessageProperties> messagePropertiesKafkaTransactionManager()
	{
		KafkaTransactionManager<String,EbMSMessageProperties> ktm = new KafkaTransactionManager<String,EbMSMessageProperties>(messagePropertiesProducerFactory());
		ktm.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		return ktm;
	}
}
