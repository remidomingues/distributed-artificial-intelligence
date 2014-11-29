/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 * 
 * GNU Lesser General Public License
 * 
 * This library is free software; you can redistribute it and/orog
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

import homework3.CloningBehaviour;
import homework1.model.Artifact;
import homework1.model.ArtifactCategory;
import homework1.model.ArtifactDescription;
import homework1.model.ArtifactGenre;
import homework1.model.AuctionDescription;
import homework3.AuctionResult;
import homework3.ContainerManager;
import homework3.HomingBehaviour;
import homework3.MobileAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * Curator agent, manage the following requests:
 * - Artifact details request: artifact ID -> artifact details
 * - Artifact selection request: List<ArtifactGender, ArtifactCategory> -> List<Artifact ID>
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class CuratorAgent extends MobileAgent {
    /** Logger */
    protected Logger myLogger = Logger.getJADELogger(getClass().getName());
    /** Artifacts data */
    private Map<Integer, Artifact> artifacts = new HashMap<Integer, Artifact>();
    /** List of agents subscribed to the auction **/
    public Set<AID> subscribedAgents = new HashSet<>();
    public AuctionResult auctionResult = null;
    private Artifact auctionedArtifact = null;
    private double currentAuctionPrice;
    private double currentAuctionReserve;
    private boolean auctionBehaviour = false;
    private boolean parallelAuctionBehaviour = false;
    private boolean clonedAuctionBehaviour = false;
    public boolean auctionDone = false;
    public List<AuctionResult> results = new LinkedList<AuctionResult>();

    /**
     * Constructor
     */
    public CuratorAgent() {
        artifacts.put(1, new Artifact(1, "Mario Bros", "Shigeru Miyamoto", new GregorianCalendar(1983, 1, 1), "Japan", ArtifactGenre.Game, ArtifactCategory.Science));
        artifacts.put(2, new Artifact(2, "Le Penseur", "Auguste Rodin", new GregorianCalendar(1902, 1, 1), "France", ArtifactGenre.Sculpture, ArtifactCategory.Philosophy));
        artifacts.put(3, new Artifact(3, "Roman empire", "Volubilis", new GregorianCalendar(-300, 1, 1), "Morocco", ArtifactGenre.Misc, ArtifactCategory.Archeology));
        ArtifactCategory[] categories = ArtifactCategory.values();
        ArtifactGenre[] genres = ArtifactGenre.values();
        String[] firstnames = {"Leonardo", "Gustav", "Vlad", "Nicolas", "Genghis", "Albert", "Michael", "Sebastian"};
        String[] lastnames = {"Da Vinci", "Klimt", "Kush", "Poussin", "Khan", "Einstein", "Angelo", "Kruger"};
        String[] places = {"France","Germany","Italy", "Greece", "USA", "Sweden"};
        Random random = new Random();
        for(int i = 4; i < 100; i++) {
            ArtifactCategory category = (ArtifactCategory) pickRandom(categories, random);
            ArtifactGenre genre = (ArtifactGenre) pickRandom(genres, random);
            String firstname = (String) pickRandom(firstnames, random);
            String lastname = (String) pickRandom(lastnames, random);
            String place = (String) pickRandom(places, random);
            String authorName = firstname + " " + lastname;
            int year = 300 + random.nextInt(1500);
            int month = 1 + random.nextInt(11);
            int day = 1 + random.nextInt(28);
            String artifactName = "Random artifact #" + i;

            artifacts.put(i, new Artifact(i, artifactName, authorName, new GregorianCalendar(year, month, day), place, genre, category));
        }
    }
    
    private void publishAuctioneerService() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("CuratorAuctioneer");
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            System.out.println(getLocalName() + ": Agent registered to Directory Facilitator");
            DFService.register(this,dfd);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Could not register to DF", e);
            doDelete();
        }
    }
    
    protected void setup() {
        Object[] args = getArguments();
        System.out.println(getLocalName() + ": Initializing Curator agent in <Main-Container>");
        subscribedAgents.clear();
        
        if(args != null && args.length == 1) {
            if(args[0].equals("auction")){
                this.auctionBehaviour = true;
            } else if(args[0].equals("parallel-auction")) {
                this.parallelAuctionBehaviour = true;
                this.clonedAuctionBehaviour = false;
            }
        } else if(this.getLocalName().contains("clone")) {
                this.clonedAuctionBehaviour = true;
                this.parallelAuctionBehaviour = false;
        }
        
        myLogger.log(Logger.INFO, "Auction behaviour: " + this.auctionBehaviour);
        
        if(this.auctionBehaviour || this.parallelAuctionBehaviour || this.clonedAuctionBehaviour) {
      
            if(this.parallelAuctionBehaviour) {
                
                // Picking the auctionned artifact
                Artifact auctionnedArtifact = (Artifact) CuratorAgent.pickRandom(getArtifacts().toArray(), new Random());
                // Estimating the initial auction price and it's reserve (minimum price)
                setAuctionedArtifact(auctionnedArtifact);

                //Main program
                try {
                    AgentContainer ac = ContainerManager.createContainer(this);
                    ac.createNewAgent("MuseoGalileo", "homework1.ProfilerAgent", new Object[]{"parallel-auction"});
                    ac.getAgent("MuseoGalileo").start();
                    
                    AgentContainer ac2 = ContainerManager.createContainer(this);
                    ac2.createNewAgent("HeritageMalta", "homework1.ProfilerAgent", new Object[]{"parallel-auction"});
                    ac2.getAgent("HeritageMalta").start();
                } catch (StaleProxyException ex) {
                    java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ControllerException ex) {
                    java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                //ContainerManager.moveToNewContainer(this);
                this.addBehaviour(new CloningBehaviour(this));
            }
            // Starting auction messages' handling
            publishAuctioneerService();
            this.addBehaviour(new CuratorAuctioningBehaviour(this));
        } else {
            this.addBehaviour(new CuratorRequestsHandlingBehaviour(this));
        }
    }
    
    static public Object pickRandom(Object[] objects, Random random) {
        return objects[new Random().nextInt(objects.length)];
    }

    static protected double estimatePrice(Artifact artifact) {
        Calendar today = Calendar.getInstance();  
        int age = today.get(Calendar.YEAR) - artifact.getCreatedAt().get(Calendar.YEAR);
        return 50.0 + 10.0 * age + 60.0;
    }
    
    /**
     * Return a collection of artifacts
     * @return A collection of artifacts
     */
    public Collection<Artifact> getArtifacts() {
        return artifacts.values();
    }
    
    /**
     * Return a list of agents that are currently subscribed to the auctioning
     * @return A list of agents' names
     */
    public Set<AID> getSubscribedAgents() {
        return this.subscribedAgents;
    }

    public Artifact getAuctionedArtifact() {
        return auctionedArtifact;
    }

    public double getCurrentAuctionPrice() {
        return currentAuctionPrice;
    }

    public double getCurrentAuctionReserve() {
        return currentAuctionReserve;
    }
    
    public void setAuctionedArtifact(Artifact auctionedArtifact) {
        this.auctionedArtifact = auctionedArtifact;
    }

    public void setCurrentAuctionPrice(double currentAuctionPrice) {
        this.currentAuctionPrice = currentAuctionPrice;
    }

    public void setCurrentAuctionReserve(double currentAuctionReserve) {
        this.currentAuctionReserve = currentAuctionReserve;
    }

    @Override
    public void sendResult() {
        System.out.println(getLocalName() + ": Sending result to parent");
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        AgentMessage result = new AgentMessage("result", this.auctionResult);
        try {
            message.setContentObject(result);
        } catch (IOException ex) {
            myLogger.log(Level.SEVERE, null, ex);
        }
        message.addReceiver(new AID(getLocalName().split("#")[1], false));
        send(message);
        this.doDelete();
    }

    /**
     * Interacts with the profiling agents bidding on an auction
     */
    private class CuratorAuctioningBehaviour extends CyclicBehaviour {
        /**
         * Constructor
         * @param a Agent
         */            
        public CuratorAuctioningBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            if(results.size() == ((CuratorAgent)myAgent).children+1) {
                String s = getLocalName() + ": All results received for artifact " + results.get(0).artifactID + ":\n";
                for(AuctionResult ar : results) {
                    s += "- " + ar.winnerName + " is willing to pay " + ar.price + "\n";
                }
                System.out.println(s);
                myAgent.doDelete();
            }
            
            CuratorAgent curatorAgent = (CuratorAgent) myAgent;
            ACLMessage  msg = myAgent.blockingReceive(10);
            

            if (msg == null){
                return;
            }

            AgentMessage agentMessage;
            try {
                agentMessage = (AgentMessage) msg.getContentObject();
            } catch (UnreadableException ex) {
                myLogger.log(Logger.SEVERE, "<AUCTION> Exception while reading received object message", ex);
                return;
            }

            myLogger.log(Logger.INFO, "<AUCTION> Agent {0} - Received <{1}> from {2}", new Object[]{getLocalName(), agentMessage.getType(), msg.getSender().getLocalName()});

            // Main logic
            Artifact currentAuction = curatorAgent.getAuctionedArtifact();
            if (agentMessage.getType().equals("auction-registration") && msg.getPerformative() == ACLMessage.REQUEST) {
                curatorAgent.getSubscribedAgents().add(msg.getSender());
                if(curatorAgent.subscribedAgents.size() >= 2) {
                    startAuction();
                }
            } else if (agentMessage.getType().equals("auction-accept") && msg.getPerformative() == ACLMessage.PROPOSE) {
                // Checking that the proposal matches the current price
                AuctionDescription auctionDescription = (AuctionDescription) agentMessage.getContent();

                if (currentAuction == null || auctionDescription.getPrice() != curatorAgent.getCurrentAuctionPrice()) {
                    myLogger.log(Logger.INFO, "<AUCTION> Auction proposal received for a terminated auction from " + msg.getSender().getName());
                    // The price is different from the current bidding price or there's no auction anymore
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    try {
                        reply.setContentObject(new AgentMessage("wrong-price", auctionDescription));
                    } catch (IOException ex) {
                        myLogger.log(Level.SEVERE, null, ex);
                    }
                    send(reply);
                    return;
                }

                // Ending the auction
                curatorAgent.setAuctionedArtifact(null);
                myLogger.log(Logger.INFO, "<AUCTION> Auction proposal from " + msg.getSender().getName() + "accepted");
                
                curatorAgent.auctionResult = new AuctionResult(auctionDescription.getArtifactID(), msg.getSender().getLocalName(), auctionDescription.getPrice());
                System.out.println(getLocalName() + ": Best offer for artifact " + auctionDescription.getArtifactID() + " is " + auctionDescription.getPrice() +
                        " from " + msg.getSender().getName());
                results.add(auctionResult);
                
                // Letting the agent know that his proposal was accepted
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                try {
                    reply.setContentObject(new AgentMessage("accepted-bid", auctionDescription));
                } catch (IOException ex) {
                    myLogger.log(Level.SEVERE, null, ex);
                }
                send(reply);

                // Broadcasting a message informing all the subscribed agents that the auction ended
                ACLMessage broadcastedMessage = new ACLMessage(ACLMessage.INFORM);
                AgentMessage endMessage = new AgentMessage("auction-end", currentAuction.getId());
                curatorAgent.auctionDone = true;
                try {
                    broadcastedMessage.setContentObject(endMessage);
                } catch (IOException ex) {
                    myLogger.log(Level.SEVERE, null, ex);
                }
                for (AID agent : curatorAgent.getSubscribedAgents()) {
                    broadcastedMessage.addReceiver(agent);
                }
                send(broadcastedMessage);
                
            } else if (agentMessage.getType().equals("result") && msg.getPerformative() == ACLMessage.INFORM) {
                AuctionResult result = (AuctionResult)agentMessage.getContent();
                System.out.println(getLocalName() + ": Received <result> from from " + msg.getSender().getLocalName()
                    + ": Best buyer for artifact " + result.artifactID + ": " + result.winnerName + " (price=" + result.price + ")");
                results.add(result);
            }
        }
    }

    private void startAuction() {
        String s = getLocalName()+ ": Starting auction for artifact " + auctionedArtifact.getId()
                + ". Buyers are ";
        for(AID a : subscribedAgents) {
            s += a.getLocalName() + ", ";
        }
        
        System.out.println(s.substring(0, s.length()-2));
        double estimatedPrice = CuratorAgent.estimatePrice(auctionedArtifact);
        setCurrentAuctionPrice(estimatedPrice*2.0);
        setCurrentAuctionReserve(estimatedPrice*0.3);

        // Sending an "auction-start" to all subscribed agents
        ACLMessage broadcastedMessage = new ACLMessage(ACLMessage.INFORM);
        AgentMessage auctionStart = new AgentMessage("auction-start", auctionedArtifact);
        try {
            broadcastedMessage.setContentObject(auctionStart);
        } catch (IOException ex) {
            myLogger.log(Level.SEVERE, null, ex);
        }
        for (AID agent : getSubscribedAgents()) {
            broadcastedMessage.addReceiver(agent);
        }
        send(broadcastedMessage);
        addBehaviour(new CuratorAuctionRound(this));
    }



    /**
     * A behaviour that models the auction's rounds.
     * At each tick, the price of the auctionned artifact is lowered
     * if the auction is still going on
     */
    private class CuratorAuctionRound extends TickerBehaviour {
        static private final long ROUND_DURATION = 500;

        /**
         * Constructor
         * @param a Agent
         */            
        public CuratorAuctionRound(Agent a) {
            super(a, ROUND_DURATION);
        }

        @Override
        public void onTick() {
            CuratorAgent curatorAgent = (CuratorAgent) myAgent;

            // If the current auction have finished...
            if (curatorAgent.getAuctionedArtifact() == null) {
                if(subscribedAgents.isEmpty()) {
                    return;
                }
                if(curatorAgent.auctionDone) {
                    if(curatorAgent.clonedAuctionBehaviour) {
                        myAgent.addBehaviour(new HomingBehaviour(myAgent));
                    }
                    this.done();
                    return;
                }
                // Launching a new auction
                curatorAgent.startAuction();
                this.done();
                return;
            }


            // Updating the auction's price
            double auctionPrice = curatorAgent.getCurrentAuctionPrice() * 0.9;

            // If the decreased price is still higher than the reserve...
            if (auctionPrice > curatorAgent.getCurrentAuctionReserve()) {
                curatorAgent.setCurrentAuctionPrice(auctionPrice);

                // Broadcasts the new auction price
                ACLMessage broadcastedMessage = new ACLMessage(ACLMessage.INFORM);
                AuctionDescription auctionDescription = new AuctionDescription(curatorAgent.getAuctionedArtifact().getId(), auctionPrice);
                AgentMessage priceMessage = new AgentMessage("auction-price", auctionDescription);
                try {
                    broadcastedMessage.setContentObject(priceMessage);
                } catch (IOException ex) {
                    myLogger.log(Level.SEVERE, null, ex);
                }
                for (AID agent : curatorAgent.getSubscribedAgents()) {
                    broadcastedMessage.addReceiver(agent);
                }
                send(broadcastedMessage);
            } else {
                // If the price is lower than the reserve, we inform everyone that the auction ended
                Artifact endedAuction = curatorAgent.getAuctionedArtifact();
                curatorAgent.setAuctionedArtifact(null);

                // Broadcasting a message informing all the subscribed agents that the auction ended
                ACLMessage broadcastedMessage = new ACLMessage(ACLMessage.INFORM);
                AgentMessage endMessage = new AgentMessage("auction-end", endedAuction.getId());
                curatorAgent.auctionDone = true;
                try {
                    broadcastedMessage.setContentObject(endMessage);
                } catch (IOException ex) {
                    myLogger.log(Level.SEVERE, null, ex);
                }
                for (AID agent : curatorAgent.getSubscribedAgents()) {
                    broadcastedMessage.addReceiver(agent);
                }
                send(broadcastedMessage);
            }


        }
    }
    
    
    /**
     * Wait for an ACLMessage request and answer it according to the request
     * defined for the given agent
     */
    private class CuratorRequestsHandlingBehaviour extends CyclicBehaviour {
        /**
         * Constructor
         * @param a Agent
         */
        public CuratorRequestsHandlingBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            Integer id = null;
            ACLMessage  msg = myAgent.blockingReceive();
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if(msg.getPerformative() == ACLMessage.REQUEST){
                    Object content = null;
                    
                    try {
                        content = msg.getContentObject();
                    } catch (UnreadableException ex) {
                        java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, "Could not read object content from message", ex);
                    }
                    
                    if(content != null && content instanceof AgentMessage){
                        AgentMessage message = (AgentMessage)content;
                        
                        //Artifact details request from ProfilerAgent
                        if(message.getType().equals("get-details")){
                            this.answerArtifactDetailsMessage(reply, message, msg.getSender().getLocalName());
                            
                        //Artifact selection request based on genre and category from TourGuideAgent
                        } else if(message.getType().equals("get-artifacts")){
                            this.answerArtifactSelectionMessage(reply, message, msg.getSender().getLocalName());
                        } else{
                            myLogger.log(Logger.INFO, "Agent {0} - Unexpected request type [{1}] received", new Object[]{getLocalName(), message.getType()});
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("(UnexpectedType ("+message.getType()+"))");
                        }
                    }
                    else{
                        myLogger.log(Logger.INFO, "Agent {0} - Unexpected request [{1}] received from {2}", new Object[]{getLocalName(), content, msg.getSender().getLocalName()});
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("(UnexpectedContent ("+content+"))");
                    }

                }
                else {
                    myLogger.log(Logger.INFO, "Agent {0} - Unexpected message [{1}] received from {2}", new Object[]{getLocalName(), ACLMessage.getPerformative(msg.getPerformative()), msg.getSender().getLocalName()});
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");   
                }
                send(reply);
            }
            else {
                block();
            }
        }
   
        /**
         * Parse an artifact selection request and build the appropriate answer
         * @param reply
         * @param request
         * @param sender 
         */
        private void answerArtifactSelectionMessage(ACLMessage reply, AgentMessage request, String sender) {
            myLogger.log(Logger.INFO, "Agent {0} - Received get-artifacts request from {1}", new Object[]{getLocalName(), sender});
            reply.setPerformative(ACLMessage.INFORM);

            //Parse message
            List<ArtifactDescription> descriptions = null;
            try {
                descriptions = (List<ArtifactDescription>)request.getContent();
            } catch(Exception e) {
                myLogger.log(Logger.INFO, "Agent {0} - Unexpected request content [{1}] received", new Object[]{getLocalName(), request.getContent()});
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("(UnexpectedContent ("+request.getContent()+"))");
            }

            //Build response
            try {
                List<Integer> artifacts = new LinkedList<>();
                for(ArtifactDescription desc : descriptions) {
                    for(Artifact a : ((CuratorAgent)this.myAgent).getArtifacts()) {
                        if(a.getCategory() == desc.getCategory() && a.getGenre() == desc.getGenre() && !artifacts.contains(a)) {
                            artifacts.add(a.getId());
                            break;
                        }
                    }
                }
                reply.setContentObject(new AgentMessage("get-artifacts", artifacts));
                
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, "Could not serialize artifact ID list ", ex);
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("(Internal error)");
            }
        }
        
        /**
         * Parse an artifact details request and build the appropriate answer
         * @param reply
         * @param request
         * @param sender 
         */
        private void answerArtifactDetailsMessage(ACLMessage reply, AgentMessage request, String sender) {
            myLogger.log(Logger.INFO, "Agent {0} - Received get-details request from {1}", new Object[]{getLocalName(), sender});
            reply.setPerformative(ACLMessage.INFORM);

            //Parse message
            int id;
            try {
                id = (int)request.getContent();
            } catch(Exception e) {
                myLogger.log(Logger.INFO, "Agent {0} - Unexpected request content [{1}] received", new Object[]{getLocalName(), request.getContent()});
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("(UnexpectedContent ("+request.getContent()+"))");
                return;
            }

            //Build response
            try {
                reply.setContentObject(new AgentMessage("get-details", artifacts.get(id)));
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, "Could not serialize artifact "+id, ex);
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("(Internal error)");
            }
        }
    }
}
