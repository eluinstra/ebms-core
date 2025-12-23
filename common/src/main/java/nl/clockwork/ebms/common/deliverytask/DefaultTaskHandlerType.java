package nl.clockwork.ebms.common.deliverytask;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class DefaultTaskHandlerType implements Condition
{
  @Override
  public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata)
  {
    return context.getEnvironment().getProperty("deliveryTaskHandler.start", Boolean.class, true)
        && context.getEnvironment().getProperty("deliveryTaskHandler.type", DeliveryTaskHandlerType.class, DeliveryTaskHandlerType.DEFAULT)
            == DeliveryTaskHandlerType.DEFAULT;
  }
}
