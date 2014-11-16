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
import homework1.model.Gender;
import homework1.model.User;
import homework1.model.Occupation;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;


public class ProfilerAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());

    private User user;
    public ProfilerAgent() {
        myLogger.log(Logger.INFO, "Profile Agent initialized");
    }
    
    public User getUser() {
        return this.user;
    }

    private class RequestVirtualTourBehaviour extends OneShotBehaviour {

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
            ACLMessage  response = myAgent.blockingReceive();
            
            if (response == null){
                block();
                return;
            }
      
            if(response.getPerformative() != ACLMessage.INFORM) {
                myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(response.getPerformative()) + "] received from " + response.getSender().getLocalName());
                return;
            }
            
            AgentMessage agentResponse = null;
            LinkedList<Integer> artifactIds;
            try {
                agentResponse = (AgentMessage) response.getContentObject();
                artifactIds = (LinkedList<Integer>) agentResponse.getContent();
            } catch (UnreadableException ex) {
                myLogger.log(Logger.SEVERE, "Exception while reading received object message (artifacts id)", ex);
                return;
            }
            
            myLogger.log(Logger.INFO, "Agent {0} - Received <{1}:INFORM> from {2}", new Object[]{getLocalName(), agentResponse.getType(), response.getSender().getLocalName()});

            // Creating a sequential behaviour (visiting the artifacts one after the other)
            SequentialBehaviour visitingBehaviour = new SequentialBehaviour(myAgent);
            for(Integer artifactId : artifactIds) {
                visitingBehaviour.addSubBehaviour(new VisitingArtifactBehaviour(myAgent, artifactId));
            }
            myLogger.log(Logger.INFO, "Number of artifacts to visit: " + artifactIds.size());

            // ... and a final waker behaviour to request a new tour after a while
            visitingBehaviour.addSubBehaviour(new NewTourBehaviour(myAgent));
            
            // Adding the sequential behaviour to the agent
            myAgent.addBehaviour(visitingBehaviour);
        }
    } // END of inner class RequestVirtualTourBehaviour
    
    private class NewTourBehaviour extends WakerBehaviour {
        public final static long NEW_TOUR_DELAY = 4000;
        public NewTourBehaviour(Agent a) {
            super(a, NEW_TOUR_DELAY);
        }

        public void onWake() {
            myLogger.log(Logger.INFO, "!! Finished current tour, requesting a new tour");
            myAgent.addBehaviour(new RequestVirtualTourBehaviour(myAgent));
        }
    }
    
    private class VisitingArtifactBehaviour extends OneShotBehaviour {
        public final static long VISITING_DELAY = 2000;
        
        private int artifactId;
        public VisitingArtifactBehaviour(Agent a, int artifactId) {
            super(a);
            this.artifactId = artifactId;
        }

        public void action() {
            ProfilerAgent profileAgent = (ProfilerAgent) myAgent;            
                        
            // Getting full details for the current artifact
            ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
            requestMessage.addReceiver(new AID("curator", false));
            
            AgentMessage agentMsg = new AgentMessage("get-details", this.artifactId);

            try {
                requestMessage.setContentObject(agentMsg);
            } catch (IOException ex) {
                myLogger.log(Logger.SEVERE, "Exception while sending object message (artifact id)", ex);
                return;
            }
            send(requestMessage);

            // Getting response from the tour-guide
            ACLMessage  response = myAgent.blockingReceive();
            
            if (response == null){
                block();
                return;
            }
      
            if(response.getPerformative() != ACLMessage.INFORM) {
                myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(response.getPerformative()) + "] received from " + response.getSender().getLocalName());
                return;
            }
            
            AgentMessage agentResponse = null;
            Artifact artifact;
            try {
                agentResponse = (AgentMessage) response.getContentObject();
                artifact = (Artifact) agentResponse.getContent();
            } catch (UnreadableException ex) {
                myLogger.log(Logger.SEVERE, "Exception while reading received object message (artifacts id)", ex);
                return;
            }

            myLogger.log(Logger.INFO, "Agent {0} - Received <{1}:INFORM> from {2}", new Object[]{getLocalName(), agentResponse.getType(), response.getSender().getLocalName()});
            // Ignoring artifacts that were already visited
            Boolean unvisited = true;
            for(Artifact a: profileAgent.getUser().getVisitedArtifacts()){
                if (a.getId() == artifact.getId()) {
                    unvisited = false;
                    break;
                }
            }
            if (unvisited) {
                myLogger.log(Logger.INFO, "* Visiting artifact '" + artifact.getName() + "' made by '" + artifact.getAuthor() + "'");

                // Adding the artifacts to the visited artifacts
                profileAgent.getUser().getVisitedArtifacts().add(artifact);
            }
            
            // Artificial sleep to simulate user looking at info then moving to the next artifact
            try {
                Thread.sleep(VISITING_DELAY);
            } catch (InterruptedException ex) {
                myLogger.log(Level.SEVERE, null, ex);
            }
        }
    } // END of inner class RequestVirtualTourBehaviour
    
    protected void setup() {
        // Getting arguments
        // Example arguments: MALE,UNEMPLOYED,21,Mythology,Science
        Object[] args = getArguments();

        if (args == null || args.length < 4) {
            myLogger.log(Logger.SEVERE, "Didn't pass enough arguments to the Profile Agent, falling back to default arguments.");
            // Putting default parameters
            String[] defaultArguments = {"MALE", "UNEMPLOYED", "21", "Mythology", "Science"};
            args = (Object[]) defaultArguments;
        }

        Gender gender = Gender.valueOf((String) args[0]);
        Occupation occupation = Occupation.valueOf((String) args[1]);
        int age = Integer.parseInt((String) args[2]);
        
        LinkedList<ArtifactCategory> interests = new LinkedList<>();
          
        for (int i = 3; i < args.length; i++) {
            String interest = (String) args[i];
            interests.add(ArtifactCategory.valueOf(interest));
        }
        
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

