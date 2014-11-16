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

import homework1.AgentMessage;

import homework1.model.Artifact;
import homework1.model.ArtifactCategory;
import homework1.model.ArtifactDescription;
import homework1.model.ArtifactGenre;
import homework1.model.Gender;
import homework1.model.User;
import homework1.model.Occupation;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class TourGuideAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    public static final int NTHREADS = 5;

    public TourGuideAgent() {
        myLogger.log(Logger.INFO, "Tour Guide Agent initialized");
    }

    private class TourBuilderBehaviour extends CyclicBehaviour {

        public TourBuilderBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            //Wait message from ProfileAgent
            ACLMessage  msg = myAgent.receive();
            
            if(msg == null){
                myLogger.log(Logger.SEVERE, "Null message received from {0}", msg.getSender().getLocalName());
                return;
            }
            
            AgentMessage message = null;
            try {
                message = ((AgentMessage)msg.getContentObject());
            } catch(Exception e) {
                myLogger.log(Logger.SEVERE, "Invalid message object received from "+msg.getSender().getLocalName(), e);
                return;
            }
            
            if(message.getType().equals("get-tour")) {
                User user = null;
                try {
                    user = (User) message.getContent();
                } catch(Exception e) {
                    myLogger.log(Logger.SEVERE, "Invalid message content received from "+msg.getSender().getLocalName(), e);
                    return;
                }
                
                List<ArtifactDescription> descriptions = new LinkedList<ArtifactDescription>();
                for(ArtifactCategory categ : user.getInterests()) {
                    for(ArtifactGenre genre : ArtifactGenre.values()) {
                        descriptions.add(new ArtifactDescription(categ, genre));
                    }
                }

                //Send artifacts ID request to CuratorAgent
                ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
                requestMessage.addReceiver(new AID("curator", false));

                AgentMessage agentMsg = new AgentMessage("GET", descriptions);

                try {
                    requestMessage.setContentObject(agentMsg);
                } catch (IOException ex) {
                    myLogger.log(Logger.SEVERE, "Exception while serializing object message (artifacts description)", ex);
                    return;
                }
                send(requestMessage);
                
            } else if(message.getType().equals("GET")) {
                List<Integer> artifacts = null;
                try {
                    artifacts = (List<Integer>) message.getContent();
                } catch(Exception e) {
                    myLogger.log(Logger.SEVERE, "Invalid message content received from "+msg.getSender().getLocalName(), e);
                    return;
                }
                
                //Send artifacts ID response to ProfilerAgent
                ACLMessage requestMessage = new ACLMessage(ACLMessage.INFORM);
                requestMessage.addReceiver(new AID("profiler", false));

                AgentMessage agentMsg = new AgentMessage("get-tour", artifacts);

                try {
                    requestMessage.setContentObject(agentMsg);
                } catch (IOException ex) {
                    myLogger.log(Logger.SEVERE, "Exception while serializing object message (artifacts ID)", ex);
                    return;
                }
                send(requestMessage);
                
            } else {
                myLogger.log(Logger.SEVERE, "Invalid message type received from {0}", msg.getSender().getLocalName());
            }
        }
    } // END of inner class TourBuilderBehaviour

    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("TourGuideAgent");
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            ParallelBehaviour parallelBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);
            for(int i = 0; i < NTHREADS; ++i) {
                parallelBehaviour.addSubBehaviour(new TourBuilderBehaviour(this));
            }
            this.addBehaviour(parallelBehaviour);
            DFService.register(this,dfd);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Could not register with DF", e);
            doDelete();
        }
    }
}
