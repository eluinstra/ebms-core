package nl.clockwork.ebms.common.deliverytask;

import java.time.Duration;

import lombok.Value;

@Value
public class DeliveryChannel
{
  boolean isReliableMessaging;
  int retries;
  Duration retryInterval;
}
