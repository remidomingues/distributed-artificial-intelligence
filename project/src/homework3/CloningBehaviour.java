/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework3;

import homework1.CuratorAgent;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import jade.util.Logger;
import jade.wrapper.AgentContainer;

/**
 *
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class CloningBehaviour extends OneShotBehaviour {  
    /** Logger */
    protected Logger myLogger = jade.util.Logger.getJADELogger(getClass().getName());
    
    public CloningBehaviour(Agent a) {
        super(a);
    }
    
    @Override
    public void action() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(CloningBehaviour.class.getName()).log(Level.SEVERE, null, ex);
        }
        ContainerManager.requestContainers(myAgent);
        
        MessageTemplate mt = MessageTemplate.and(
			                  MessageTemplate.MatchSender(myAgent.getAMS()),
			                  MessageTemplate.MatchPerformative(ACLMessage.INFORM));
         ACLMessage resp = myAgent.blockingReceive(mt);
         ContentElement ce = null;
        try {
            ce = myAgent.getContentManager().extractContent(resp);
        } catch (Codec.CodecException ex) {
            Logger.getLogger(CloningBehaviour.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OntologyException ex) {
            Logger.getLogger(CloningBehaviour.class.getName()).log(Level.SEVERE, null, ex);
        }
        Result result = (Result) ce;
        jade.util.leap.Iterator it = result.getItems().iterator();
        int i = 1;
        String containerName = ContainerManager.requestAgentContainer(myAgent, myAgent.getAID()).getName();
        while (it.hasNext()) {
            Location loc = (Location)it.next();
            if(!loc.getName().equals(containerName)) {
                String cloneName = "clone" + i + "#" + myAgent.getLocalName();
                System.out.println("Cloning agent <" + myAgent.getLocalName() + "> into clone <" + cloneName + "> and sending clone to <" + loc.getName() + ">");
                myAgent.doClone(loc, cloneName);
                /*
                for(AgentContainer ac : CuratorAgent.agentContainers) {
                    try {
                        ac.getAgent(cloneName).start();
                    } catch(Exception e) {}
                }*/
                ++i;
            }
        }
        ((MobileAgent)myAgent).children = result.getItems().size()-1;
    }
}
