/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework3;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import jade.util.Logger;

/**
 *
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class HomingBehaviour extends OneShotBehaviour {  
    /** Logger */
    protected Logger myLogger = jade.util.Logger.getJADELogger(getClass().getName());
    
    public HomingBehaviour(Agent a) {
        super(a);
    }
    
    @Override
    public void action() {
        Location loc = ContainerManager.requestAgentContainer(myAgent, new AID(myAgent.getLocalName().split("#")[1], false));
        System.out.println("Moving agent <" + myAgent.getLocalName() + "> back to destination " + loc.getName());
        myAgent.doMove(loc);   
        ((MobileAgent)myAgent).sendResult();
    }
}
