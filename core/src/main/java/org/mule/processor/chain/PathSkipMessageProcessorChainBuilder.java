package org.mule.processor.chain;

import org.mule.api.processor.MessageProcessorChain;

/**
 * Created by martinbuchwald on 8/29/16.
 */
public class PathSkipMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder {
    @Override
    protected MessageProcessorChain buildMessageProcessorChain(DefaultMessageProcessorChain chain)
    {
        return new InterceptingChainLifecycleWrapperPathSkip(chain, processors, name);
    }
}
