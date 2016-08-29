package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;

import java.util.List;

/**
 * Created by martinbuchwald on 8/29/16.
 */
public class InterceptingChainLifecycleWrapperPathSkip extends InterceptingChainLifecycleWrapper{

    public InterceptingChainLifecycleWrapperPathSkip(MessageProcessorChain chain,
                                                     List<MessageProcessor> processors,
                                                     String name)
    {
        super(chain, processors, name);
    }

}
