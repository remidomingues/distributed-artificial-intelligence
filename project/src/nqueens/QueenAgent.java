package nqueens;


import homework1.model.Artifact;
import homework1.model.ArtifactCategory;
import homework1.model.AuctionDescription;
import homework1.model.Gender;
import homework1.model.User;
import homework1.model.Occupation;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class QueenAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    private List<AID> previousQueens = new LinkedList<AID>();

    private int mNumQueens;
    private int mQueenIndex;
    private AID mPreviousQueen;
    
    public QueenAgent() {
    }
    
    
    public Logger getLogger() {
        return myLogger;
    }

    public int getNumQueens() {
        return mNumQueens;
    }

    public int getQueenIndex() {
        return mQueenIndex;
    }
    
    private class PositionNextQueen extends OneShotBehaviour {

        public PositionNextQueen(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            QueenAgent qAgent = (QueenAgent) myAgent;     
            
            // Pick a valid position
            // ! TODO !

            // If this is the last queen, finish
            if (qAgent.getQueenIndex() >= qAgent.getNumQueens() - 1) {
                return;
            }
            
            // Create a new queen and asking it to position itself in a valid position
            ContainerController cc = getContainerController();
            int newQueenIndex = qAgent.getQueenIndex() + 1;
            AgentController ac;
            try {
                ac = cc.createNewAgent("queen" + newQueenIndex, "nqueens.QueenAgent", new Object[]{newQueenIndex, qAgent.getAID()});
                ac.start();
            } catch (StaleProxyException ex) {
                myLogger.log(Logger.SEVERE, null, ex);
                return;
            }
            
            ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
            requestMessage.addReceiver(new AID("placenextqueensnamehere", false));
            
            AgentMessage agentMsg = new AgentMessage("take-position", null);

            try {
                requestMessage.setContentObject(agentMsg);
            } catch (IOException ex) {
                myLogger.log(Logger.SEVERE, "Exception while sending object message", ex);
                return;
            }
            send(requestMessage);
        }
    } // END of inner class PositionNextQueen
    
    protected void setup() {
        // Getting arguments
        Object[] args = getArguments();
        
        mNumQueens = 8;
        mQueenIndex = 0;
        mPreviousQueen = null;
        
        if (args != null) {
            if (args.length >= 1) {
                mQueenIndex = (Integer) args[0];
            }
            if (args.length >= 2) {
                mPreviousQueen = (AID) args[1];
            }
        }
        
        myLogger.log(Logger.INFO, "Queen Agent initialized with: " + mQueenIndex + "/" + mNumQueens);

        addBehaviour(new PositionNextQueen(this));       
    }
}

