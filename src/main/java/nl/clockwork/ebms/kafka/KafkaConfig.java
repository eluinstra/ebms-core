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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.val;
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

@Configuration
@Conditional(DeliveryTaskHandlerConfig.KafkaTaskHandlerType.class)
@EnableKafka
public class KafkaConfig
{
	private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

	@Value("${kafka.serverUrl}")
	private String kafkaServerUrl;

	@PostConstruct
	public void init()
	{
		logger.info("init<KafkaConfig>");
	}

	@Bean
	public ProducerFactory<String, EbMSMessageProperties> messagePropertiesProducerFactory()
	{
		return new DefaultKafkaProducerFactory<>(messageProducerConfiguration());
	}

	@Bean
	public ProducerFactory<String, DeliveryTask> deliveryTaskProducerFactory()
	{
		return new DefaultKafkaProducerFactory<>(deliveryTaskProducerConfiguration());
	}

	@Bean(name = "deliveryTaskKafkaTemplate")
	public KafkaTemplate<String, DeliveryTask> deliveryTaskKafkaTemplate()
	{
		logger.info("Initializing deliveryTaskKafkaTemplate");
		return new KafkaTemplate<>(deliveryTaskProducerFactory());
	}

	@Bean(name = "messagePropertiesKafkaTemplate")
	public KafkaTemplate<String, EbMSMessageProperties> messagePropertiesKafkaTemplate()
	{
		logger.info("Initializing messagePropertiesKafkaTemplate");
		return new KafkaTemplate<>(messagePropertiesProducerFactory());
	}

	@Bean
	public Map<String, Object> deliveryTaskProducerConfiguration()
	{
		val result = new HashMap<String, Object>();
		result.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUrl);
		result.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		result.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		result.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		result.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
		result.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "delivery-task-transaction");
		return result;
	}

	@Bean
	public Map<String, Object> messageProducerConfiguration()
	{
		val result = new HashMap<String, Object>();
		result.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUrl);
		result.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		result.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		result.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		result.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
		result.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "message-property-transaction");
		return result;
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, DeliveryTask> kafkaListenerContainerFactory()
	{
		val result = new ConcurrentKafkaListenerContainerFactory<String, DeliveryTask>();
		result.setConsumerFactory(consumerFactory());
		return result;
	}

	@Bean
	public DefaultKafkaConsumerFactory<String, DeliveryTask> consumerFactory()
	{
		val deliveryTaskDeserializer = new JsonDeserializer<>(DeliveryTask.class);
		deliveryTaskDeserializer.addTrustedPackages("nl.clockwork.ebms.delivery.task.DeliveryTask");
		return new DefaultKafkaConsumerFactory<>(consumerConfigurations(), new StringDeserializer(), deliveryTaskDeserializer);
	}

	@Bean
	public Map<String, Object> consumerConfigurations()
	{
		val result = new HashMap<String, Object>();
		result.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUrl);
		result.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
		result.put(ConsumerConfig.GROUP_ID_CONFIG, "EBMS");
		result.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		result.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
		result.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		result.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		result.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
		return result;
	}

	@Bean
	public KafkaTransactionManager<String, DeliveryTask> deliveryTaskKafkaTransactionManager()
	{
		KafkaTransactionManager<String, DeliveryTask> result = new KafkaTransactionManager<>(deliveryTaskProducerFactory());
		result.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		return result;
	}

	@Bean
	public KafkaTransactionManager<String, EbMSMessageProperties> messagePropertiesKafkaTransactionManager()
	{
		KafkaTransactionManager<String, EbMSMessageProperties> result = new KafkaTransactionManager<>(messagePropertiesProducerFactory());
		result.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		return result;
	}
}
