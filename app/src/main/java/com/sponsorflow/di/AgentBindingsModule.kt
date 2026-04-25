package com.sponsorflow.di

import com.sponsorflow.agents.SponsorflowAgent
import com.sponsorflow.agents.action.BehaviorSimulationAgent
import com.sponsorflow.agents.action.CommsAgent
import com.sponsorflow.agents.action.ComposerAgent
import com.sponsorflow.agents.action.PublisherAgent
import com.sponsorflow.agents.direction.BuddyReviewerAgent
import com.sponsorflow.agents.cognitive.CatalogAgent
import com.sponsorflow.agents.cognitive.GreetingAgent
import com.sponsorflow.agents.cognitive.OrderParsingAgent
import com.sponsorflow.agents.cognitive.PlanningAgent
import com.sponsorflow.agents.cognitive.PolicyAgent
import com.sponsorflow.agents.cognitive.ReasoningAgent
import com.sponsorflow.agents.cognitive.SynthesizerAgent
import com.sponsorflow.agents.cognitive.UserLearningAgent
import com.sponsorflow.agents.direction.CommandHandlerAgent
import com.sponsorflow.agents.direction.RouterAgent
import com.sponsorflow.agents.kairos.PrivacyAgent
import com.sponsorflow.agents.kairos.MemoryAgent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentBindingsModule {

    @Binds
    @IntoMap
    @StringKey("MemoryAgent")
    abstract fun bindMemoryAgent(agent: MemoryAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("PublisherAgent")
    abstract fun bindPublisherAgent(agent: PublisherAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("CatalogAgent")
    abstract fun bindCatalogAgent(agent: CatalogAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("PolicyAgent")
    abstract fun bindPolicyAgent(agent: PolicyAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("GreetingAgent")
    abstract fun bindGreetingAgent(agent: GreetingAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("OrderParsingAgent")
    abstract fun bindOrderParsingAgent(agent: OrderParsingAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("PlanningAgent")
    abstract fun bindPlanningAgent(agent: PlanningAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("ReasoningAgent")
    abstract fun bindReasoningAgent(agent: ReasoningAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("UserLearningAgent")
    abstract fun bindUserLearningAgent(agent: UserLearningAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("ComposerAgent")
    abstract fun bindComposerAgent(agent: ComposerAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("BuddyReviewerAgent")
    abstract fun bindBuddyReviewerAgent(agent: BuddyReviewerAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("PrivacyAgent")
    abstract fun bindPrivacyAgent(agent: PrivacyAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("RouterAgent")
    abstract fun bindRouterAgent(agent: RouterAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("SynthesizerAgent")
    abstract fun bindSynthesizerAgent(agent: SynthesizerAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("BehaviorSimulationAgent")
    abstract fun bindBehaviorSimulationAgent(agent: BehaviorSimulationAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("CommsAgent")
    abstract fun bindCommsAgent(agent: CommsAgent): SponsorflowAgent

    @Binds
    @IntoMap
    @StringKey("CommandHandlerAgent")
    abstract fun bindCommandHandlerAgent(agent: CommandHandlerAgent): SponsorflowAgent
}
