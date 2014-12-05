/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework3;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.ProfileImpl;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent managing the containers
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class ContainerManager {
    //Parallel Auctions
    private static void sendRequest(Agent agent, Action action) {
      agent.getContentManager().registerLanguage(new SLCodec());
      agent.getContentManager().registerOntology(MobilityOntology.getInstance());
          
      ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
      request.setLanguage(new SLCodec().getName());
      request.setOntology(MobilityOntology.getInstance().getName());
      try {
	     agent.getContentManager().fillContent(request, action);
	     request.addReceiver(action.getActor());
	     agent.send(request);
        } catch (Exception ex) { ex.printStackTrace(); }
   }
    
    public static void requestContainers(Agent agent) {
        ContainerManager.sendRequest(agent, new Action(agent.getAMS(), new QueryPlatformLocationsAction()));
    }
    
    public static Location requestAgentContainer(Agent agent, AID agentRequested) {
        WhereIsAgentAction action = new WhereIsAgentAction();
        action.setAgentIdentifier(agentRequested);
        ContainerManager.sendRequest(agent, new Action(agent.getAMS(), action));
        
        MessageTemplate mt = MessageTemplate.and(
			                  MessageTemplate.MatchSender(agent.getAMS()),
			                  MessageTemplate.MatchPerformative(ACLMessage.INFORM));
         ACLMessage resp = agent.blockingReceive(mt);
         ContentElement ce = null;
        try {
            ce = agent.getContentManager().extractContent(resp);
        } catch (Codec.CodecException ex) {
            jade.util.Logger.getLogger(HomingBehaviour.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OntologyException ex) {
            jade.util.Logger.getLogger(HomingBehaviour.class.getName()).log(Level.SEVERE, null, ex);
        }
        Result result = (Result) ce;
        return (Location) result.getValue();
    }
    
    public static AgentContainer createContainer(Agent agent) {
        agent.getContentManager().registerLanguage(new SLCodec());
        agent.getContentManager().registerOntology(MobilityOntology.getInstance());
        return jade.core.Runtime.instance().createAgentContainer(new ProfileImpl());

    }
}
