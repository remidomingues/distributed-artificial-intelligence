package nqueens;

import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


public class QueenAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    private List<Position> mPositionnedQueens = new LinkedList<>();

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

    public AID getPreviousQueen() {
        return mPreviousQueen;
    }

    public List<Position> getPositionnedQueens() {
        return mPositionnedQueens;
    }
    
    private class PositionNextQueen extends OneShotBehaviour {

        public PositionNextQueen(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            QueenAgent qAgent = (QueenAgent) myAgent;     
            
            // Pick a valid position
            int y = qAgent.getQueenIndex();
            for (int x = 0; x < qAgent.getNumQueens(); x++) {
                Boolean vulnerable = false;
                for (Position p : mPositionnedQueens) {
                    if (x == p.getX() || y == p.getY() ||  (x - p.getX()) == (y - p.getY())) {
                        vulnerable = true;
                        break;
                    }
                }
                if (vulnerable) {
                    continue;
                }
                Position pickedPosition = new Position(x, y);
                mPositionnedQueens.add(pickedPosition);

                myLogger.log(Logger.INFO, qAgent.getLocalName() + " chose position " + pickedPosition.toString());
                myLogger.log(Logger.INFO, mPositionnedQueens.toString());


                // If this is the last queen, we've found a solution.
                // TODO : send a message to the previous queen saying a solution was found
                // the message should be forwarded to all the other previous queens...
                if (qAgent.getQueenIndex() >= qAgent.getNumQueens() - 1) {
                    sendMessage(mPreviousQueen, "solution-found", mPositionnedQueens);
                    qAgent.doDelete();
                    try {
                        // Waiting for the agent to be deleted
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        myLogger.log(Logger.SEVERE, null, ex);
                    }
                    return;
                }

                // Otherwise, create a new queen and asking it to position itself in a valid position
                ContainerController cc = getContainerController();
                int newQueenIndex = qAgent.getQueenIndex() + 1;
                AgentController ac;
                try {
                    ac = cc.createNewAgent("queen" + newQueenIndex, "nqueens.QueenAgent", new Object[]{newQueenIndex, qAgent.getAID(), mPositionnedQueens});
                    ac.start();
                } catch (StaleProxyException ex) {
                    myLogger.log(Logger.SEVERE, null, ex);
                    return;
                }

                // Wait for an answer (solution found, or no possible solution)
                ACLMessage response = qAgent.blockingReceive();
                AgentMessage msg;
                try {
                    msg = (AgentMessage) response.getContentObject();
                } catch (UnreadableException ex) {
                    myLogger.log(Logger.SEVERE, null, ex);
                    return;
                }
                // If a solution found, we forward it to the previous queen (if any)
                if (msg.getType().equals("solution-found")) {
                    AID previous = qAgent.getPreviousQueen();
                    if (previous != null) {
                        sendMessage(previous, "solution-found", msg.getContent());                            
                    } else {
                        // if this is the first queen, we announce the solution
                        LinkedList<Position> solution = (LinkedList<Position>) msg.getContent();
                        myLogger.log(Logger.INFO, "A solution was found:\n" + qAgent.solutionToString(solution));
                    }
                } else if (msg.getType().equals("no-possible-solution")) {
                    // If no solution was found by the new queen, we remove the current x and move to a new one
                    mPositionnedQueens.remove(mPositionnedQueens.size() -1);
                } else {
                    myLogger.log(Logger.SEVERE, "Unexpected msg type: " + msg.getType());
                    return;
                }
            }
            
            // If we got out of this loop == no possible solution was found. We tell that to the previous queen... and suicide.
            sendMessage(qAgent.getPreviousQueen(), "no-possible-solution", null);
            qAgent.doDelete();
        }
    } // END of inner class PositionNextQueen
    
    protected void sendMessage(AID receiver, String type, Object content) {
        ACLMessage requestMessage = new ACLMessage(ACLMessage.INFORM);
        requestMessage.addReceiver(receiver);
        AgentMessage agentMsg = new AgentMessage(type, content);
        try {
            requestMessage.setContentObject(agentMsg);
        } catch (IOException ex) {
            myLogger.log(Logger.SEVERE, "Exception while sending object message of type '"+type+"'", ex);
            return;
        }
        send(requestMessage);
    }
    
    public String solutionToString(List<Position> solution) {
        StringBuilder sb = new StringBuilder();
        for (Position p : solution) {
            for (int x = 0; x < mNumQueens; x++) {
                if (p.getX() == x) {
                    sb.append("X ");
                } else {
                    sb.append(". ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    protected void setup() {
        // Getting arguments
        Object[] args = getArguments();
        
        mNumQueens = 4;
        mQueenIndex = 0;
        mPreviousQueen = null;
        
        if (args != null) {
            if (args.length >= 1) {
                mQueenIndex = (Integer) args[0];
            }
            if (args.length >= 2) {
                mPreviousQueen = (AID) args[1];
            }
            if (args.length >= 3) {
                mPositionnedQueens = (LinkedList<Position>) args[2];
            }
        }
        
        myLogger.log(Logger.INFO, "Queen Agent initialized with: " + mQueenIndex + "/" + mNumQueens);

        addBehaviour(new PositionNextQueen(this));       
    }
}

