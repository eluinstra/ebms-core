package nl.clockwork.ebms.common.deliverytask;

import static lombok.AccessLevel.PRIVATE;

import java.time.Instant;

import org.quartz.JobDataMap;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class DeliveryTaskTools
{
	public static DeliveryTask createDeliveryTask(final JobDataMap properties)
	{
		return new DeliveryTask(
				properties.getString("cpaId"),
				properties.getString("sendDeliveryChannel"),
				properties.getString("receiveDeliveryChannel"),
				properties.getString("messageId"),
				properties.get("timeToLive") != null ? Instant.ofEpochMilli(properties.getLong("timeToLive")) : null,
				Instant.ofEpochMilli(properties.getLong("timestamp")),
				properties.getBoolean("isConfidential"),
				properties.getInt("retries"));
	}
}
