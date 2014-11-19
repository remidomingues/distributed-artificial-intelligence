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
import homework1.model.AuctionDescription;
import homework1.model.Gender;
import homework1.model.User;
import homework1.model.Occupation;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.SubscriptionInitiator;
import jade.util.Logger;
import jade.util.leap.Iterator;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;


public class ProfilerAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    private List<AID> tourGuideAgents = new LinkedList<AID>();
    private AID selectedTourGuideAgent = null;
    private Map<Integer, Double> currentAuctions = new HashMap<Integer, Double>();

    private User user;
    public ProfilerAgent() {
        myLogger.log(Logger.INFO, "Profile Agent initialized");
    }
    
    public User getUser() {
        return this.user;
    }
    
    public Logger getLogger() {
        return myLogger;
    }
    
    public void addTourGuideService(AID aid) {
        tourGuideAgents.add(aid);
    }
    
    public AID getSelectedTourGuideAgent() {
        return selectedTourGuideAgent;
    }
    
    public boolean isTourGuideKnown(AID aid) {
        return tourGuideAgents.contains(aid);
    }

    private class RequestVirtualTourBehaviour extends OneShotBehaviour {
        AID aid;

        public RequestVirtualTourBehaviour(Agent a, AID aid) {
            super(a);
            this.aid = aid;
        }

        @Override
        public void action() {
            ProfilerAgent profileAgent = (ProfilerAgent) myAgent;            

            // Sending message to the tour-guide
            ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
            requestMessage.addReceiver(aid);
            
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
            myAgent.addBehaviour(new RequestVirtualTourBehaviour(myAgent, ((ProfilerAgent)myAgent).getSelectedTourGuideAgent()));
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
    
    /**
     * Dutch Auction behaviour for a buyer agent
     */
    private class DutchAuctionBuyerBehaviour extends CyclicBehaviour {
        /**
         * Constructor
         * @param a Agent
         */
        public DutchAuctionBuyerBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage  msg = myAgent.blockingReceive();
            
            if(msg != null){
                if(msg.getPerformative() == ACLMessage.INFORM){
                    Object content = null;
                    try {
                        content = msg.getContentObject();
                    } catch (UnreadableException ex) {
                        java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, "Could not read object content from message", ex);
                    }
                    
                    if(content != null && content instanceof AgentMessage){
                        AgentMessage message = (AgentMessage)content;
                        
                        //Auction initialization
                        if(message.getType().equals("auction-start")){
                            int artifactID = ((AuctionDescription)message.getContent()).getArtifactID();
                            double price = estimatePrice((Artifact)message.getContent());
                            currentAuctions.put(artifactID, price);
                            myLogger.log(Logger.INFO, String.format("Agent {0} - Auction started for artifact {1}. Estimated price: {2}",
                                    getLocalName(), artifactID, price));
                        //Auction price notifications
                        } else if(message.getType().equals("auction-price")){
                            int artifactID = ((AuctionDescription)message.getContent()).getArtifactID();
                            double price = ((AuctionDescription)message.getContent()).getPrice();
                            if(price < currentAuctions.get(artifactID)) {
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.PROPOSE);
                                try {
                                    reply.setContentObject(new AgentMessage("auction-accept", message.getContent()));
                                } catch (IOException ex) {
                                    java.util.logging.Logger.getLogger(ProfilerAgent.class.getName()).log(Level.SEVERE, "Could not serialize auction acceptance", ex);
                                }
                            }
                        //Auction end: no bids
                        } else if(message.getType().equals("auction-end")){
                            currentAuctions.remove(((Artifact)message.getContent()).getId());   
                            myLogger.log(Logger.INFO, String.format("Agent {0} - Auction ended for artifact {1}",
                                    getLocalName(), ((Artifact)message.getContent()).getId()));
                        }
                        else {
                            myLogger.log(Logger.INFO, "Agent {0} - Unexpected request type [{1}] received", new Object[]{getLocalName(), message.getType()});
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("(UnexpectedType ("+message.getType()+"))");
                        }
                    }
                    else{
                        myLogger.log(Logger.INFO, "Agent {0} - Unexpected request [{1}] received from {2}", new Object[]{getLocalName(), content, msg.getSender().getLocalName()});
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                        reply.setContent("(UnexpectedContent ("+content+"))");
                        send(reply);
                    }

                } else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    myLogger.log(Logger.INFO, "Agent {0} - Proposal accepted!", getLocalName());
                } else if(msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    myLogger.log(Logger.INFO, "Agent {0} - Proposal rejected!", getLocalName());
                }
                else {
                    myLogger.log(Logger.INFO, "Agent {0} - Unexpected message [{1}] received from {2}", new Object[]{getLocalName(), ACLMessage.getPerformative(msg.getPerformative()), msg.getSender().getLocalName()});
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");   
                    send(reply);
                }
            }
            else {
                block();
            }
        }
    }
    
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
        
        try {
            //Look for TourGuideAgents registered to the DF
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(ProfilerAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("TourGuideBuilder");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            tourGuideAgents.clear();
            for (int i = 0; i < result.length; ++i) {
                tourGuideAgents.add(result[i].getName());
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        //User interaction in order to select the right tour guide agent to request
        Scanner scanner = new Scanner(System.in);
        while(this.selectedTourGuideAgent == null) {
            System.out.println("Please select one of the Tour Guide agents below:");
            int i = 1;
            for(AID aid : tourGuideAgents) {
                System.out.println("" + i + " - " + aid.getName());
                i++;
            }
            try {
                this.selectedTourGuideAgent = tourGuideAgents.get(Integer.parseInt(scanner.nextLine())-1);
            } catch(Exception e) {
                System.out.println("Invalid agent number.");
            }
        }
        
        //Sending tour guide request to the selected agent
        addBehaviour(new RequestVirtualTourBehaviour(this, this.selectedTourGuideAgent));
        addBehaviour(new DutchAuctionBuyerBehaviour(this));
        
        //Agent subscription to the DF in order to get notified when a new
        //service according to the specified template is published
        SearchConstraints sc = new SearchConstraints();
        // We want to receive 20 results at most
        sc.setMaxResults(new Long(20));

        addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
            protected void handleInform(ACLMessage inform) {
                ProfilerAgent agent = (ProfilerAgent)myAgent;
                try {
                    DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
                    if (results.length > 0) {
                        for (int i = 0; i < results.length; ++i) {
                            DFAgentDescription dfd = results[i];
                            AID provider = dfd.getName();
                            Iterator it = dfd.getAllServices();
                            while (it.hasNext()) {
                                ServiceDescription sd = (ServiceDescription) it.next();
                                if (sd.getType().equals("TourGuideBuilder") && !agent.isTourGuideKnown(provider)) {
                                    agent.getLogger().log(Logger.INFO, "- New service published: \""+sd.getName()+"\" provided by agent "+provider.getName());
                                    agent.addTourGuideService(provider);
                                }
                            }
                        }
                    }	
                    System.out.println();
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        } );
        
        //HW2
        this.auctionRegistration();
    }
    
    //HW2
    private void auctionRegistration() {
        ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
        requestMessage.addReceiver(new AID("curator", false));

        AgentMessage agentMsg = new AgentMessage("auction-registration", getName());

        try {
            requestMessage.setContentObject(agentMsg);
        } catch (IOException ex) {
            myLogger.log(Logger.SEVERE, "Exception while registering to auction", ex);
            return;
        }
        send(requestMessage);
    }
}

