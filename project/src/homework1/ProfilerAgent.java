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
import homework1.model.Gender;
import homework1.model.User;
import homework1.model.Occupation;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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


public class ProfilerAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());

    private User user;
    public ProfilerAgent() {
        myLogger.log(Logger.INFO, "Profile Agent initialized");
    }
    
    public User getUser() {
        return this.user;
    }

    private class RequestVirtualTourBehaviour extends CyclicBehaviour {

        public RequestVirtualTourBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            ProfilerAgent profileAgent = (ProfilerAgent) myAgent;            

            // Sending message to the tour-guide
            ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
            requestMessage.addReceiver(new AID("tour-guide", false));
            
            AgentMessage agentMsg = new AgentMessage("get-tour", profileAgent.getUser());

            try {
                requestMessage.setContentObject(agentMsg);
            } catch (IOException ex) {
                myLogger.log(Logger.SEVERE, "Exception while sending object message (interests)", ex);
                return;
            }
            send(requestMessage);
            
            // Getting response from the tour-guide
            ACLMessage  response = myAgent.receive();
            
            if (response == null){
                block();
                return;
            }
      
            if(response.getPerformative() != ACLMessage.INFORM) {
                myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(response.getPerformative()) + "] received from " + response.getSender().getLocalName());
                return;
            }
            
            LinkedList<Integer> artifactIds;
            try {
                AgentMessage agentResponse = (AgentMessage) response.getContentObject();
                artifactIds = (LinkedList<Integer>) agentResponse.getContent();
            } catch (UnreadableException ex) {
                myLogger.log(Logger.SEVERE, "Exception while reading received object message (artifacts id)", ex);
                return;
            }

            VisitingArtifactsBehaviour visitingBehaviour = new VisitingArtifactsBehaviour(myAgent, artifactIds);
            myAgent.addBehaviour(visitingBehaviour);
        }
    } // END of inner class RequestVirtualTourBehaviour

    
    private class VisitingArtifactsBehaviour extends TickerBehaviour {
        public final static long VISITING_DELAY = 2;
        
        private LinkedList<Integer> artifactsIds;
        private int currentArtifactIndex = 0;
        public VisitingArtifactsBehaviour(Agent a, LinkedList<Integer> artifactsIds) {
            super(a, VISITING_DELAY);
            
            this.artifactsIds = artifactsIds;
            this.currentArtifactIndex = 0;
        }

        public void onTick() {
            ProfilerAgent profileAgent = (ProfilerAgent) myAgent;            

            // If we visited all artifacts, stop the behaviour
            if(this.currentArtifactIndex > this.artifactsIds.size() - 1) {
                this.done();
            }
            
            int currentArtifactId = this.artifactsIds.get(this.currentArtifactIndex);
            
            // Getting full details for the current artifact
            ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
            requestMessage.addReceiver(new AID("curator", false));
            
            AgentMessage agentMsg = new AgentMessage("DET", currentArtifactId);

            try {
                requestMessage.setContentObject(agentMsg);
            } catch (IOException ex) {
                myLogger.log(Logger.SEVERE, "Exception while sending object message (artifact id)", ex);
                return;
            }
            send(requestMessage);

            // Getting response from the tour-guide
            ACLMessage  response = myAgent.receive();
            
            if (response == null){
                block();
                return;
            }
      
            if(response.getPerformative() != ACLMessage.INFORM) {
                myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(response.getPerformative()) + "] received from " + response.getSender().getLocalName());
                return;
            }
            
            Artifact artifact;
            try {
                AgentMessage agentResponse = (AgentMessage) response.getContentObject();
                artifact = (Artifact) agentResponse.getContent();
            } catch (UnreadableException ex) {
                myLogger.log(Logger.SEVERE, "Exception while reading received object message (artifacts id)", ex);
                return;
            }

            myLogger.log(Logger.INFO, "* Visiting artifact '" + artifact.getName() + "' made by '" + artifact + "'");
        }
    } // END of inner class RequestVirtualTourBehaviour
    
    protected void setup() {
        // Getting arguments
        // Example arguments: MALE,UNEMPLOYED,21,Mythology,Science
        Object[] args = getArguments();

        if (args == null || args.length < 4) {
            myLogger.log(Logger.SEVERE, "Didn't pass any/enough arguments to the Profile Agent");
            return;
        }

        Gender gender = Gender.valueOf((String) args[0]);
        Occupation occupation = Occupation.valueOf((String) args[1]);
        int age = Integer.parseInt((String) args[2]);
        
        LinkedList<ArtifactCategory> interests = new LinkedList<ArtifactCategory>();
          
        for (int i = 3; i < args.length; i++) {
            String interest = (String) args[i];
            interests.add(ArtifactCategory.valueOf(interest));
        }

        LinkedList<Artifact> visitedArtifacts = new LinkedList<Artifact>();
        
        this.user = new User(gender, occupation, age, interests);
        
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ProfileAgent");
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
            RequestVirtualTourBehaviour PingBehaviour = new  RequestVirtualTourBehaviour(this);
            addBehaviour(PingBehaviour);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }
}

