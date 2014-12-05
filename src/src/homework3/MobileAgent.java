/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework3;

import jade.core.Agent;

/**
 *
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public abstract class MobileAgent extends Agent {
    public int children = Integer.MAX_VALUE;
    
    public MobileAgent() {
        
    }

    @Override
    protected void afterClone() {
        setup();
    }
    
    public abstract void sendResult();
}
