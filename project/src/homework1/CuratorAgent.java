/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 * 
 * GNU Lesser General Public License
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 * **************************************************************
 */
package homework1;

import homework1.model.Artifact;
import homework1.model.ArtifactCategory;
import homework1.model.ArtifactDescription;
import homework1.model.ArtifactGenre;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
   This example shows a minimal agent that just prints "Hallo World!" 
   and then terminates.
   @author Giovanni Caire - TILAB
 */

public class CuratorAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    private Map<Integer, Artifact> artifacts = new HashMap<Integer, Artifact>();
    
    public CuratorAgent() {
        artifacts.put(1, new Artifact(1, "Mario Bros", "Shigeru Miyamoto", new GregorianCalendar(1983, 1, 1), "Japan", ArtifactGenre.Game, ArtifactCategory.Science));
        artifacts.put(2, new Artifact(2, "Le Penseur", "Auguste Rodin", new GregorianCalendar(1902, 1, 1), "France",ArtifactGenre.Sculpture, ArtifactCategory.Philosophy));
        
        this.addBehaviour(new ArtifactsDetailsProviderBehaviour(this));
        myLogger.log(Logger.INFO, "Curator Agent initialized");
    }
        
    private class ArtifactsDetailsProviderBehaviour extends CyclicBehaviour {

        public ArtifactsDetailsProviderBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            Integer id = null;
            ACLMessage  msg = myAgent.receive();
            if(msg != null){
                ACLMessage reply = msg.createReply();

                if(msg.getPerformative() == ACLMessage.REQUEST){
                    /*
                    //STUFF TO ADD TO MANAGE THE ARTIFACT SELECTION
                    List<ArtifactDescription> descriptions = null;
                    try {
                        descriptions = (List<ArtifactDescription>) msg.getContentObject();
                    } catch(Exception e) {
                    }
                    
                    if(descriptions != null){
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received DET request from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.INFORM);
                        try {
                            reply.setContentObject(artifacts.get(id));
                        } catch (IOException ex) {
                            java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, "Could not serialize artifact "+id, ex);
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("(Internal error)");
                        }
                    */
                    String content = msg.getContent();
                    try {
                        id = Integer.parseInt(content.split(" ")[1]);
                    } catch(Exception e) {
                    }
                    if((content != null) && (content.indexOf("DET") != -1) && id != null){
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received DET request from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.INFORM);
                        try {
                            reply.setContentObject(artifacts.get(id));
                        } catch (IOException ex) {
                            java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, "Could not serialize artifact "+id, ex);
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("(Internal error)");
                        }
                    }
                    else{
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected request ["+content+"] received from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("(UnexpectedContent ("+content+"))");
                    }

                }
                else {
                    myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");   
                }
                send(reply);
            }
            else {
                block();
            }
        }
    }
}
